package it.eably.backend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Requires a running PostgreSQL instance")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
