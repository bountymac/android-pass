import studio.forface.easygradle.dsl.implementation

plugins {
    id("org.jetbrains.kotlin.jvm")
}   

dependencies {
    implementation(projects.pass.domain)
}
