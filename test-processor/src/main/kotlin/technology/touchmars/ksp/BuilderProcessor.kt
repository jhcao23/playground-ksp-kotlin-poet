package technology.touchmars.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import java.io.IOException
import java.io.OutputStream
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction

fun OutputStream.appendText(str: String) {
    this.write(str.toByteArray())
}
class BuilderProcessor(
    val options: Map<String, String>,
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger
) : SymbolProcessor {

    private var log: OutputStream = codeGenerator.createNewFile(
        Dependencies(false),
        "technology.touchmars.ksp", "BuilderProcessor", "log"
    )
    private var invoked = false
    fun emit(s: String, indent: String = "") {
        try {
            log.appendText("$indent$s\n")
        } catch (e: IOException) {
            e.printStackTrace()
            log.close()
        }

    }
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) {
            return emptyList()
        }
        // Test second Annotation
        val builderClass = JavaBuilderProcessor::class
        for (m in builderClass.memberFunctions) {
            val srcElement = m::javaMethod.get()?.kotlinFunction ?: continue
            emit(srcElement.toString())
        }
        emit("technology.touchmars.ksp.TestProcessor: init($options)", "")
        val goodGuyNodes = resolver.getSymbolsWithAnnotation(GoodGuy::class.asClassName().canonicalName)
        goodGuyNodes.forEach {
            if (it is KSClassDeclaration)
                emit("goodguy: ${it.containingFile?.filePath}")
            else if (it is KSFunctionDeclaration)
                emit("goodguy fun: " +
                    "${it.simpleName.asString()}::" +
                    "${ClassName.bestGuess(it.returnType!!.resolve().declaration.qualifiedName!!.asString())}"
                )
        }
        emit("")
        // Test end

        val pkgName = Builder::class.asClassName().canonicalName
        val symbols = resolver.getSymbolsWithAnnotation(pkgName)
        val ret = symbols.filter { !it.validate() }.toList()
        symbols
            .filter { it is KSClassDeclaration && it.validate() }
            .forEach { it.accept(BuilderVisitor(), Unit) }
        if (ret.isEmpty()) {
            invoked = true
        } else {
            emit("leftover: ${ret.size}")
            emit(ClassName.bestGuess((ret[0] as KSClassDeclaration).qualifiedName!!.asString()).canonicalName)
        }
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
                val pType = ClassName.bestGuess(it.type.resolve().declaration.qualifiedName!!.asString())
                    .copy(true)
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
            val fileOutput = codeGenerator.createNewFile(
                Dependencies(true, function.containingFile!!), packageName , className
            )
            val fileSpec = fileSpecBuilder.addType(typeSpecBuilder.build()).build()
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