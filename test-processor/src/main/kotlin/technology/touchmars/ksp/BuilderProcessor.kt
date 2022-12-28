package technology.touchmars.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.FileSpec
import java.io.OutputStream

fun OutputStream.appendText(str: String) {
    this.write(str.toByteArray())
}
class BuilderProcessor(
    val options: Map<String, String>,
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val pkgName = Builder::class.asClassName().canonicalName
        val symbols = resolver.getSymbolsWithAnnotation(pkgName)
        val ret = symbols.filter { !it.validate() }.toList()
        symbols
            .filter { it is KSClassDeclaration && it.validate() }
            .forEach { it.accept(BuilderVisitor(), Unit) }
        return ret
    }

    inner class BuilderVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            classDeclaration.primaryConstructor!!.accept(this, data)
        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            val parent = function.parentDeclaration as KSClassDeclaration
            val packageName = parent.containingFile!!.packageName.asString()
            val targetClassName = parent.simpleName.asString()
            val className = "${targetClassName}Builder"
            val parameters = function.parameters
            // kotlin poet
            val fileSpecBuilder = FileSpec.builder(packageName, className)
            val typeSpecBuilder = TypeSpec.classBuilder(className)
            parameters.forEach {
                val pName = it.name!!.getShortName()
                val pType = ClassName.bestGuess(it.type.resolve().declaration.qualifiedName!!.asString()).copy(true) // ClassName.bestGuess(it.type.toString()).copy(true)
                typeSpecBuilder.addProperty(
                    PropertySpec.builder(pName, pType, KModifier.PRIVATE)
                        .mutable()
                        .initializer("null")
                        .build()
                )
                val parameterSpec = ParameterSpec.builder(pName, pType).build()
                typeSpecBuilder.addFunction(
                    FunSpec.builder("with${pName.replaceFirstChar { it.uppercase() } }")
                        .addParameter(parameterSpec)
                        .addModifiers(KModifier.INTERNAL)
                        .addStatement("this.%N = %N", pName, pName)
                        .addStatement("return this")
                        .returns(ClassName(packageName, "$className"))
                        .build()
                )
            }
            val statements = parameters.map { it.name!!.getShortName() }.joinToString("!!, ") + "!!"
            typeSpecBuilder.addFunction(
                FunSpec.builder("build")
                    .addModifiers(KModifier.INTERNAL)
                    .returns(ClassName.bestGuess(parent.qualifiedName!!.asString()))
                    .addStatement("return ${targetClassName}($statements)")
                    .build()
            )
            val fileOutput = codeGenerator.createNewFile(Dependencies(true, function.containingFile!!), packageName , className)

            val fileSpec = fileSpecBuilder
                .addType(typeSpecBuilder.build())
                .build()
            fileOutput.appendText(fileSpec.toString())
            fileOutput.close()

        }
    }

}

class BuilderProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return BuilderProcessor(environment.options, environment.codeGenerator, environment.logger)
    }
}