package technology.touchmars.ksp

import com.squareup.kotlinpoet.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement

class JavaBuilderProcessor : AbstractProcessor() {

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }
    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf("technology.touchmars.ksp.Builder")
    }

    private fun processType(typeElement: TypeElement) {
        val typeSpecBuilder = TypeSpec.classBuilder("${typeElement.simpleName}Builder")
        val constructor = typeElement.enclosedElements.filterIsInstance<ExecutableElement>()
            .first { it.simpleName.toString() == "<init>" }

        constructor.parameters.forEach {
            val typeName = it.asType().asTypeName()
            typeSpecBuilder.addProperty(
                PropertySpec.builder(it.simpleName.toString(), typeName, KModifier.PRIVATE)
                    .build()
            )
            val parameterSpec = ParameterSpec.builder(it.simpleName.toString(), typeName).build()
            typeSpecBuilder.addFunction(
                FunSpec.builder("with${it.simpleName.toString().replaceFirstChar { it.uppercase() } }")
                    .addParameter(parameterSpec)
                    .addModifiers(KModifier.PUBLIC)
                    .addStatement("this.\$N = \$N", it.simpleName, it.simpleName)
                    .addStatement("return this")
                    .returns(
                        ClassName((typeElement.enclosingElement as PackageElement).qualifiedName.toString(),
                        "${typeElement.qualifiedName}Builder")
                    )
                    .build()
            )
        }
        val statements = constructor.parameters.map { it.simpleName.toString() }.joinToString(", ")
        typeSpecBuilder.addFunction(
            FunSpec.builder("build")
                .addModifiers(KModifier.PUBLIC)
                .returns(typeElement.asType().asTypeName())
                .addStatement("return ${typeElement.simpleName}($statements)")
                .build()
        )
        val fileSpecBuilder = FileSpec.builder((typeElement.enclosingElement as PackageElement).qualifiedName.toString(), typeSpecBuilder.build().name!!)
        fileSpecBuilder
            .addImport("java.lang.*", "*")
            .build()
            .writeTo(processingEnv.filer)

    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        val elements = roundEnv!!.getElementsAnnotatedWith(Builder::class.java)
        elements.filterIsInstance<TypeElement>()
            .map { processType(it) }
        return true
    }

}