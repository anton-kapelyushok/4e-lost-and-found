package fe.lnf

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(TestcontainersConfig::class)
class FeLnfApplicationTests {

    @Test
    fun contextLoads() {
    }

}
