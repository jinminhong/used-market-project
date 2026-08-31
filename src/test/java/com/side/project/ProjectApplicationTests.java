package com.side.project;

import com.side.project.config.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfig.class)
@SpringBootTest
class ProjectApplicationTests {

	@Test
	void contextLoads() {
	}

}
