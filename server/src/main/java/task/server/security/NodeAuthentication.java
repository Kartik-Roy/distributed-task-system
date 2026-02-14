package task.server.security;


import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class NodeAuthentication extends AbstractAuthenticationToken {

    private final String nodeId;

    public NodeAuthentication(String nodeId) {
        super(List.of(new SimpleGrantedAuthority("ROLE_NODE")));
        this.nodeId = nodeId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return nodeId;
    }

    public String nodeId() {
        return nodeId;
    }
}
