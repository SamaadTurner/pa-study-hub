// The backend is one project, in app/. Its dependencies, test settings and the
// coverage rule are all in app/build.gradle.kts.
//
// This file only says which plugin versions the build uses.

plugins {
    id("org.springframework.boot") version "3.2.2" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
}
