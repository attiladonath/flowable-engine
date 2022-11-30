/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.ui.idm.conf;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import org.flowable.idm.api.IdmIdentityService;
import org.flowable.ui.common.properties.FlowableRestAppProperties;
import org.flowable.ui.common.security.ApiHttpSecurityCustomizer;
import org.flowable.ui.common.security.DefaultPrivileges;
import org.flowable.ui.common.security.SecurityConstants;
import org.flowable.ui.idm.properties.FlowableIdmAppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Based on http://docs.spring.io/spring-security/site/docs/3.2.x/reference/htmlsingle/#multiple-httpsecurity
 *
 * @author Joram Barrez
 * @author Tijs Rademakers
 * @author Filip Hrisafov
 */
@Configuration(proxyBeanMethods = false)
public class IdmSecurityConfiguration {

    //
    // GLOBAL CONFIG
    //

    @Autowired
    protected IdmIdentityService identityService;
    
    @Autowired
    protected FlowableIdmAppProperties idmAppProperties;
    
    @Bean
    public org.flowable.ui.idm.security.UserDetailsService userDetailsService() {
        org.flowable.ui.idm.security.UserDetailsService userDetailsService = new org.flowable.ui.idm.security.UserDetailsService();
        userDetailsService.setUserValidityPeriod(idmAppProperties.getSecurity().getUserValidityPeriod());
        return userDetailsService;
    }

    //
    // BASIC AUTH
    //

    @Configuration(proxyBeanMethods = false)
    public static class IdmApiWebSecurityConfigurationAdapter {

        protected final FlowableRestAppProperties restAppProperties;
        protected final FlowableIdmAppProperties idmAppProperties;
        protected final ApiHttpSecurityCustomizer apiHttpSecurityCustomizer;

        public IdmApiWebSecurityConfigurationAdapter(FlowableRestAppProperties restAppProperties,
                FlowableIdmAppProperties idmAppProperties, ApiHttpSecurityCustomizer apiHttpSecurityCustomizer) {
            this.restAppProperties = restAppProperties;
            this.idmAppProperties = idmAppProperties;
            this.apiHttpSecurityCustomizer = apiHttpSecurityCustomizer;
        }

        @Bean
        @Order(SecurityConstants.IDM_API_SECURITY_ORDER)
        public SecurityFilterChain idmApiSecurity(HttpSecurity http) throws Exception {

            http
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                    .csrf()
                    .disable();

            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl idmHttpRequestsConfigurer = http
                    .securityMatcher(antMatcher("/api/idm/**"))
                    .authorizeHttpRequests()
                    .requestMatchers(antMatcher("/api/idm/**"));
            if (idmAppProperties.isRestEnabled()) {

                if (restAppProperties.isVerifyRestApiPrivilege()) {
                    idmHttpRequestsConfigurer.hasAuthority(DefaultPrivileges.ACCESS_REST_API);
                } else {
                    idmHttpRequestsConfigurer.authenticated();

                }

                apiHttpSecurityCustomizer.customize(http);
                
            } else {
                idmHttpRequestsConfigurer.denyAll();

            }

            return http.build();
        }
    }
}
