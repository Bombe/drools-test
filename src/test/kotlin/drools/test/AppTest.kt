package drools.test

import org.drools.core.io.impl.ClassPathResource
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.kie.api.KieServices
import org.kie.api.management.GAV
import org.kie.api.runtime.KieSession
import kotlin.test.Test

class DroolsTest {

    @Test
    fun `slightly more complex set of rules`() {
        createSessionFrom(ClassPathResource("drools/test/test.drl"))
                .use {
                    insert(Employee(1, false, 10))
                    insert(Employee(2, false, 20))
                    insert(Employee(3, true, 30))

                    insert(RaiseSalaryRequest(1, false))
                    insert(RaiseSalaryRequest(2, true))

                    fireAllRules()

                    getObjects { true }.forEach(::println)

                    assertThat(getObjects { it is Employee && it.id == 1 }, contains<Any>(Employee(1, false, 11)))
                    assertThat(getObjects { it is Employee && it.id == 2 }, contains<Any>(Employee(2, false, 21)))
                    assertThat(getObjects { it is Employee && it.id == 3 }, contains<Any>(Employee(3, true, 33)))
                    assertThat(getObjects { it !is Employee }, emptyIterable<Any>())
                }
    }

}

fun createSessionFrom(ruleResource: ClassPathResource): KieSession =
        KieServices.get().run {
            newKieModuleModel().apply {
                newKieBaseModel("Base").apply {
                    isDefault = true
                    newKieSessionModel("Session").apply {
                        isDefault = true
                    }
                }
                newKieFileSystem().apply {
                    writeKModuleXML(toXML())
                    write("src/main/resources/test-rules.drl", ruleResource)
                    generateAndWritePomXML(GAV("drools.test", "drools-test", "1.0.0"))
                    newKieBuilder(this).buildAll().results.messages.forEach(::println)
                }
            }
            newKieContainer(GAV("drools.test", "drools-test", "1.0.0"))
                    .newKieSession("Session")
        }

fun KieSession.use(block: KieSession.() -> Unit) =
        try {
            block()
        } finally {
            dispose()
        }

data class Employee(val id: Int, val exec: Boolean, var salary: Int)

data class IsExec(val e: Employee)

data class RaiseSalaryRequest(val amount: Int, val execOnly: Boolean = false)

data class IncreaseSalaryAction(val employee: Employee, val raiseSalaryRequest: RaiseSalaryRequest, var processed: Boolean = false)
