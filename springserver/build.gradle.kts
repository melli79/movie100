plugins {
	kotlin("jvm") version "2.5.0-RC"
	kotlin("plugin.spring") version "2.5.0-RC"
	id("org.springframework.boot") version "4.2.0-SNAPSHOT"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "org.grutzmann.movie100"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/snapshot") }
}

dependencies {
	implementation(project(":core"))
	implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-restclient")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")
	implementation("com.aallam.ulid:ulid-kotlin:1.6.0")
	implementation("uk.co.conoregan:themoviedbapi:3.1.0")
	implementation("org.ehcache:ehcache:3.12.0:jakarta")
	implementation("jakarta.xml.bind:jakarta.xml.bind-api")
	implementation("org.glassfish.jaxb:jaxb-runtime")
	runtimeOnly("org.xerial:sqlite-jdbc:3.54.1.0")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<ProcessResources>("processResources") {
	// Ensure web distribution task runs before springserver compiles resources
	dependsOn(":app:webApp:wasmJsBrowserDistribution")

	// Copy distribution artifacts directly into Spring Boot's static resources directory (classpath:/static/)
	from(project(":app:webApp").layout.buildDirectory.dir("dist/wasmJs/productionExecutable")) {
		into("static")
	}
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
