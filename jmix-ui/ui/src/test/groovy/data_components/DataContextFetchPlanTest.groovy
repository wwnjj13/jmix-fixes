/*
 * Copyright 2022 Haulmont.
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

package data_components

import io.jmix.core.DataManager
import io.jmix.core.EntityStates
import io.jmix.core.FetchPlan
import io.jmix.core.FetchPlanBuilder
import io.jmix.core.FetchPlans
import io.jmix.core.Id
import io.jmix.ui.model.DataComponents
import io.jmix.ui.model.DataContext
import io.jmix.ui.model.MergeOptions
import org.springframework.beans.factory.annotation.Autowired
import test_support.DataContextSpec
import test_support.entity.sales.Order

class DataContextFetchPlanTest extends DataContextSpec {

    @Autowired
    DataComponents factory
    @Autowired
    DataManager dataManager
    @Autowired
    FetchPlans fetchPlans
    @Autowired
    EntityStates entityStates

    def "test"() {
        DataContext context = factory.createDataContext()

        Order order1 = new Order(number: '1')
        dataManager.save(order1)

        def fetchPlan = fetchPlans.builder(Order)
                .add('number')
                .add('customer', FetchPlan.BASE)
                .add('orderLines', FetchPlan.BASE)
                .build()

        order1 = dataManager.load(Id.of(order1)).fetchPlan(fetchPlan).one()

        when:
        context.merge(order1, new MergeOptions(), fetchPlan)
        context.commit()

        then:
        def order = context.find(order1)

        entityStates.isLoaded(order, 'customer')
        entityStates.isLoaded(order, 'orderLines')
        !entityStates.isLoaded(order, 'user')
    }
}
