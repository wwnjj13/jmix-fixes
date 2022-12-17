package test_support.role;

import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.*;
import test_support.entity.Foo;

@ResourceRole(name = TestFooRole.CODE, code = TestFooRole.CODE)
@RowLevelRole(name = TestFooRole.CODE, code = TestFooRole.CODE)
public interface TestFooRole {
    String CODE = "TestFooRole";

    @EntityPolicy(entityClass = Foo.class,
            actions = {EntityPolicyAction.ALL})
    @EntityAttributePolicy(entityClass = Foo.class, attributes = "*",
            action = EntityAttributePolicyAction.VIEW)
    @JpqlRowLevelPolicy(entityClass = Foo.class, where = "{E}.createdByUser.userGroup = :current_user_userGroup")
    void foo();
}