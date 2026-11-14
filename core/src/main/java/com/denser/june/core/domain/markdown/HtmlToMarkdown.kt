package com.denser.june.core.domain.markdown

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

object HtmlToMarkdown {

    fun convert(html: String?): String {
        if (html.isNullOrBlank()) return ""
        val doc = Jsoup.parseBodyFragment(html)
        val body = doc.body()
        val sb = StringBuilder()
        traverse(body, sb)
        return sb.toString()
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private fun traverse(node: Node, sb: StringBuilder) {
        when (node) {
            is TextNode -> {
                sb.append(node.text())
            }
            is Element -> {
                when (node.normalName()) {
                    "p" -> {
                        for (child in node.childNodes()) traverse(child, sb)
                        sb.append("\n\n")
                    }
                    "br" -> {
                        sb.append("\n")
                    }
                    "b", "strong" -> {
                        sb.append("**")
                        for (child in node.childNodes()) traverse(child, sb)
                        sb.append("**")
                    }
                    "i", "em" -> {
                        sb.append("*")
                        for (child in node.childNodes()) traverse(child, sb)
                        sb.append("*")
                    }
                    "u" -> {
                        for (child in node.childNodes()) traverse(child, sb)
                    }
                    "s", "strike", "del" -> {
                        sb.append("~~")
                        for (child in node.childNodes()) traverse(child, sb)
                        sb.append("~~")
                    }
                    "h1" -> {
                        sb.append("\n# ")
                        for (child in node.childNodes()) traverse(child, sb)
                        sb.append("\n\n")
                    }
                    "h2" -> {
                        sb.append("\n## ")
                        for (child in node.childNodes()) traverse(child, sb)
                        sb.append("\n\n")
                    }
                    "h3" -> {
                        sb.append("\n### ")
                        for (child in node.childNodes()) traverse(child, sb)
                        sb.append("\n\n")
                    }
                    "h4" -> {
                        sb.append("\n#### ")
                        for (child in node.childNodes()) traverse(child, sb)
                        sb.append("\n\n")
                    }
                    "h5" -> {
                        sb.append("\n##### ")
                        for (child in node.childNodes()) traverse(child, sb)
                        sb.append("\n\n")
                    }
                    "h6" -> {
                        sb.append("\n###### ")
                        for (child in node.childNodes()) traverse(child, sb)
                        sb.append("\n\n")
                    }
                    "ul" -> {
                        sb.append("\n")
                        for (child in node.childNodes()) traverse(child, sb)
                        sb.append("\n")
                    }
                    "ol" -> {
                        sb.append("\n")
                        var index = 1
                        for (child in node.children()) {
                            if (child.normalName() == "li") {
                                sb.append("$index. ")
                                for (grandchild in child.childNodes()) traverse(grandchild, sb)
                                sb.append("\n")
                                index++
                            } else {
                                traverse(child, sb)
                            }
                        }
                        sb.append("\n")
                    }
                    "li" -> {
                        sb.append("- ")
                        for (child in node.childNodes()) traverse(child, sb)
                        sb.append("\n")
                    }
                    "blockquote" -> {
                        sb.append("\n> ")
                        for (child in node.childNodes()) traverse(child, sb)
                        sb.append("\n\n")
                    }
                    "code" -> {
                        sb.append("`")
                        for (child in node.childNodes()) traverse(child, sb)
                        sb.append("`")
                    }
                    "pre" -> {
                        sb.append("\n```\n")
                        for (child in node.childNodes()) traverse(child, sb)
                        sb.append("\n```\n")
                    }
                    "a" -> {
                        val href = node.attr("href")
                        sb.append("[")
                        for (child in node.childNodes()) traverse(child, sb)
                        sb.append("]($href)")
                    }
                    else -> {
                        for (child in node.childNodes()) traverse(child, sb)
                    }
                }
            }
        }
    }
}

