package com.chattriggers.bot

import com.chattriggers.bot.types.SearchTerm
import com.copperleaf.kodiak.kotlin.KotlindocInvokerImpl
import com.copperleaf.kodiak.kotlin.models.KotlinModuleDoc
import org.eclipse.jgit.api.Git
import java.io.File
import java.nio.file.Files

object KDocGenerator {
    fun getSearchTerms(): List<SearchTerm> {
        if (!CTBot.PRODUCTION)
            return emptyList()

        val docs = getDocs()
        val terms = mutableListOf<SearchTerm>()

        logInfo("Getting search terms")

        docs.classes.filter { clazz ->
            clazz.modifiers.publicMember()
        }.forEach { clazz ->
            val name = clazz.id.replace("${clazz.`package`}.", "")
            val pkg = clazz.`package`.replace('.', '/')

            val urlBase = "https://chattriggers.com/javadocs/$pkg/$name.html"

            SearchTerm(
                clazz.name,
                urlBase,
                "${clazz.kind.lowercase()} $name"
            ).run(terms::add)

            clazz.methods.filter { method ->
                method.modifiers.publicMember()
            }.map { method ->
                val url = StringBuilder(urlBase).apply {
                    append("#${method.name}-")

                    if (method.receiver != null) {
                        append(":Dreceiver-")
                    }

                    method.parameters.joinToString("-") { it.name }.run(::append)

                    append('-')
                }.toString()

                val originalReturnValue = method.returnValue.name
                val returnType = when {
                    originalReturnValue.startsWith("apply {") -> name
                    originalReturnValue == "()" -> "Unit"
                    else -> method.returnValue.signature.joinToString("") { it.text }
                }

                val descriptor = StringBuilder().apply {
                    append(clazz.name)

                    when (clazz.kind) {
                        "Object", "Enum" -> append(".")
                        "Class", "Interface" -> append("#")
                        else -> throw IllegalStateException("Unrecognized class kind: ${clazz.kind}")
                    }

                    append(method.name)
                    append("(")

                    method.parameters.joinToString {
                        it.signature.joinToString("") { c -> c.text }
                    }.run(::append)

                    append("): ")
                    append(returnType)
                }.toString()

                SearchTerm(
                    method.name,
                    url,
                    descriptor
                )
            }.run(terms::addAll)

            clazz.fields.filter { field ->
                field.modifiers.publicMember()
            }.map { field ->
                SearchTerm(
                    field.name,
                    "$urlBase#${field.name}",
                    "field ${field.name}"
                )
            }.run(terms::addAll)
        }

        logInfo("Finished getting search terms")

        return terms
    }

    private fun getDocs(): KotlinModuleDoc {
        val ctjsDir = File("./ctjs")

        logInfo("Getting KDocs.")
        logInfo("CTJS dir: ${ctjsDir.absolutePath}")
        logInfo("CTJS dir exists: ${ctjsDir.exists()}")

        if (!ctjsDir.exists()) {
            if (ctjsDir.exists()) {
                logInfo("Deleting CTJS dir")
                ctjsDir.deleteRecursively()
            }

            logInfo("Cloning ctjs repo")
            Git.cloneRepository()
                .setURI("https://github.com/ChatTriggers/ct.js.git")
                .setBranchesToClone(listOf("refs/heads/master"))
                .setBranch("refs/heads/master")
                .setDirectory(ctjsDir)
                .call()
            logInfo("ctjs repo cloned")
        }

        val cacheDir = Files.createTempDirectory("dokkaCache")
        val runner = KotlindocInvokerImpl(cacheDir)

        val outputDir = File("build/dokka").canonicalFile.apply {
            deleteRecursively()
            mkdirs()
        }

        return runner.getModuleDoc(
            listOf(File("./ctjs/src/main/kotlin").toPath()),
            outputDir.toPath()
        ) {
            Runnable {
                // Required, don't remove
                it.bufferedReader().readText()
            }
        }!!
    }

    private fun List<String>.publicMember() = !contains("internal") && !contains("private")
}
