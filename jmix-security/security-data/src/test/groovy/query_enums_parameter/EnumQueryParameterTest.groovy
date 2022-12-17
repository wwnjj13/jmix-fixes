/*
 * Copyright 2020 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package query_enums_parameter

import io.jmix.core.*
import io.jmix.core.security.AccessDeniedException
import io.jmix.core.security.InMemoryUserRepository
import io.jmix.core.security.SecurityContextHelper
import io.jmix.core.security.SystemAuthenticator
import io.jmix.security.authentication.RoleGrantedAuthority
import io.jmix.security.role.ResourceRoleRepository
import io.jmix.security.role.RowLevelRoleRepository
import io.jmix.securitydata.impl.CurrentUserQueryParamValueProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import test_support.SecurityDataSpecification
import test_support.entity.Foo
import test_support.entity.OrderInfo
import test_support.entity.TestOrder
import test_support.entity.TestUser
import test_support.entity.TestUserGroup
import test_support.role.TestDataManagerEntityOperationsCascadeRole
import test_support.role.TestDataManagerEntityOperationsRole
import test_support.role.TestFooRole

import javax.sql.DataSource

class EnumQueryParameterTest extends SecurityDataSpecification {
    @Autowired
    UnconstrainedDataManager dataManager

    @Autowired
    DataManager secureDataManager

    @Autowired
    AuthenticationManager authenticationManager

    @Autowired
    InMemoryUserRepository userRepository

    @Autowired
    ResourceRoleRepository roleRepository

    @Autowired
    Metadata metadata

    @Autowired
    AccessConstraintsRegistry accessConstraintsRegistry

    @Autowired
    DataSource dataSource

    @Autowired
    SystemAuthenticator authenticator

    @Autowired
    CurrentUserQueryParamValueProvider currentUserQueryParamValueProvider

    @Autowired
    RowLevelRoleRepository rowLevelRoleRepository

    @Autowired
    ResourceRoleRepository resourceRoleRepository

    UserDetails user1

    UserDetails user2

    UserDetails user3

    Foo foo

    Authentication systemAuthentication

    public static final String PASSWORD = "123"

    def setup() {

        user1 = metadata.create(TestUser.class)

        user1.setUsername("user1")
        user1.setActive(true)
        user1.setPassword("{noop}$PASSWORD")
        user1.setUserGroup(TestUserGroup.NEW_VALUE)
        user1.setAuthorities(RoleGrantedAuthority
                .withRowLevelRoleProvider({ rowLevelRoleRepository.getRoleByCode(it) })
                .withResourceRoleProvider({ resourceRoleRepository.getRoleByCode(it) })
                .withRowLevelRoles(TestFooRole.CODE)
                .withResourceRoles(TestFooRole.CODE)
                .build())

        user2 = metadata.create(TestUser.class)

        user2.setUsername("user2")
        user2.setActive(true)
        user2.setPassword("{noop}$PASSWORD")
        user2.setUserGroup(TestUserGroup.NEW_VALUE1)
        user2.setAuthorities(RoleGrantedAuthority
                .withRowLevelRoleProvider({ rowLevelRoleRepository.getRoleByCode(it) })
                .withResourceRoleProvider({ resourceRoleRepository.getRoleByCode(it) })
                .withRowLevelRoles(TestFooRole.CODE)
                .withResourceRoles(TestFooRole.CODE)
                .build())

        user3 = metadata.create(TestUser.class)
        user3.setUsername("user3")
        user3.setActive(true)
        user3.setPassword("{noop}$PASSWORD")
        user3.setUserGroup(TestUserGroup.NEW_VALUE)
        user3.setAuthorities(RoleGrantedAuthority
                .withRowLevelRoleProvider({ rowLevelRoleRepository.getRoleByCode(it) })
                .withResourceRoleProvider({ resourceRoleRepository.getRoleByCode(it) })
                .withRowLevelRoles(TestFooRole.CODE)
                .withResourceRoles(TestFooRole.CODE)
                .build())


        dataManager.save(user1)
        userRepository.addUser(user1)

        dataManager.save(user2)
        userRepository.addUser(user2)

        dataManager.save(user3)
        userRepository.addUser(user3)

        foo = metadata.create(Foo.class)
        foo.setCreatedByUser(user1)
        dataManager.save(foo)

        systemAuthentication = SecurityContextHelper.getAuthentication()
    }

    def cleanup() {
        SecurityContextHelper.setAuthentication(systemAuthentication)

        userRepository.removeUser(user1)
        userRepository.removeUser(user2)
        userRepository.removeUser(user3)

        new JdbcTemplate(dataSource).execute('delete from FOO')
    }


    def "Get foo entity by user1 - creator of entity"() {
        setup:
        authenticate('user1')

        when:
        def newFoo = secureDataManager.load(Id.of(foo))
                .optional()
                .orElse(null)

        then:
            newFoo == foo
    }

    def "Get foo entity by user2, another UserGroup than user1"() {
        setup:
        authenticate('user2')

        when:
        def newFoo = secureDataManager.load(Id.of(foo))
                .optional()
                .orElse(null)

        then:
        newFoo == null
    }

    def "Get foo entity by user3, the same UserGroup as user1"() {
        setup:
        authenticate('user3')

        when:
        def newFoo = secureDataManager.load(Id.of(foo))
                .optional()
                .orElse(null)

        then:
        newFoo == foo
    }

    protected void authenticate(String username) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, PASSWORD))
        SecurityContextHelper.setAuthentication(authentication)
    }
}
