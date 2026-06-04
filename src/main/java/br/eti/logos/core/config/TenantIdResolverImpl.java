package br.eti.logos.core.config;

import br.eti.logos.commons.cache.TenantIdResolver;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class TenantIdResolverImpl implements TenantIdResolver {

    @Override
    public String resolve() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            return auth.getName();
        }
        return "anonymous";
    }
}
