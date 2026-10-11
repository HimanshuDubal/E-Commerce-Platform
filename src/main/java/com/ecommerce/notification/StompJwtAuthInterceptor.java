package com.ecommerce.notification;

import java.security.Principal;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import com.ecommerce.auth.JwtService;

@Component
public class StompJwtAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public StompJwtAuthInterceptor(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            accessor = StompHeaderAccessor.wrap(message);
        }
        StompCommand command = accessor.getCommand();

        if (command == StompCommand.CONNECT) {
            authenticateConnect(accessor);
        } else if (command == StompCommand.SEND) {
            // This service currently sends server-originated notifications only.
            // Do not allow clients to publish forged order/notification events.
            throw new AccessDeniedException("Client message publishing is not enabled");
        } else if (command == StompCommand.SUBSCRIBE) {
            Principal principal = accessor.getUser();
            if (principal == null) {
                throw new AccessDeniedException("An authenticated WebSocket connection is required");
            }
            String destination = accessor.getDestination();
            if (!"/user/queue/notifications".equals(destination)) {
                throw new AccessDeniedException("Subscription destination is not allowed");
            }
        }

        return message;
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AccessDeniedException("Missing Bearer token in STOMP CONNECT headers");
        }

        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            throw new AccessDeniedException("Empty Bearer token");
        }

        try {
            String email = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            if (!jwtService.validateToken(token, userDetails)) {
                throw new AccessDeniedException("Invalid or expired WebSocket token");
            }

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            accessor.setUser(authentication);
        } catch (AccessDeniedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AccessDeniedException("Invalid or expired WebSocket token", exception);
        }
    }
}
