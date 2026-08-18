package com.vida.apirest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Necesita Postgres, JWT_SECRET y el servidor de licencias. El humo de CI son los tests unitarios en src/test.")
class ApirestApplicationTests {

	@Test
	void contextLoads() {
	}

}
