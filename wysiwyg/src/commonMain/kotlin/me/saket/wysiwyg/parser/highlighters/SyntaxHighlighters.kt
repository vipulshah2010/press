package me.saket.wysiwyg.parser.highlighters

import me.saket.wysiwyg.parser.node.Code
import me.saket.wysiwyg.parser.node.Emphasis
import me.saket.wysiwyg.parser.node.FencedCodeBlock
import me.saket.wysiwyg.parser.node.IndentedCodeBlock
import me.saket.wysiwyg.parser.node.Link
import me.saket.wysiwyg.parser.node.Node
import me.saket.wysiwyg.parser.node.Strikethrough
import me.saket.wysiwyg.parser.node.StrongEmphasis
import me.saket.wysiwyg.util.Timber
import kotlin.reflect.KClass

class SyntaxHighlighters {

  private val highlighters = mutableMapOf<KClass<out Node>, MutableList<SyntaxHighlighter<*>>>()

  private val ignoredNodeNames = arrayOf(
      "com.vladsch.flexmark.ast.Paragraph",
      "com.vladsch.flexmark.ast.Text",
      "com.vladsch.flexmark.ast.SoftLineBreak"
  )

  init {
    add(Emphasis::class, EmphasisVisitor())
    add(StrongEmphasis::class, StrongEmphasisVisitor())
    add(Link::class, LinkVisitor())
    add(Strikethrough::class, StrikethroughVisitor())
    add(Code::class, InlineCodeVisitor())
    add(IndentedCodeBlock::class, IndentedCodeBlockVisitor())
    add(FencedCodeBlock::class, FencedCodeBlockVisitor())
    //add(BlockQuote::class, BlockQuoteVisitor())
    //add(ListBlock::class, ListBlockVisitor())
    //add(ListItem::class, ListItemVisitor())
    //add(ThematicBreak::class, ThematicBreakVisitor())
    //add(Heading::class, HeadingVisitor())
  }

  /**
   * Because multiple [SyntaxHighlighter] could be present for the same [node] and
   * [SyntaxHighlighter] are allowed to have a missing visitor, this tries finds
   * the first NodeVisitor that can read [node].
   */
  @Suppress("UNCHECKED_CAST")
  fun nodeVisitor(node: Node): NodeVisitor<Node> {
    val nodeHighlighters = highlighters[node::class] as List<SyntaxHighlighter<Node>>?

    Timber.i("checking ${node::class}")

    if (nodeHighlighters != null) {
      // Intentionally using for-i loop instead of for-each or
      // anything else that creates a new Iterator under the hood.
      for (i in 0 until nodeHighlighters.size) {
        val nodeVisitor = nodeHighlighters[i].visitor(node)
        if (nodeVisitor != null) {
          return nodeVisitor
        }
      }
    }

    val isIgnoredNode = ignoredNodeNames.any { node::class.toString().contains(it) }
    if (isIgnoredNode.not()) {
      Timber.w("No visitor for node: ${node::class}")
    }

    return NodeVisitor.EMPTY
  }

  fun <T : Node> add(
    nodeType: KClass<T>,
    visitor: NodeVisitor<T>
  ) =
    add(nodeType, object : SyntaxHighlighter<T> {
      override fun visitor(node: T) = visitor
    })

  fun <T : Node> add(
    nodeType: KClass<T>,
    highlighter: SyntaxHighlighter<T>
  ) {
    if (nodeType in highlighters) {
      highlighters[nodeType]!!.add(highlighter)
    } else {
      @Suppress("ReplacePutWithAssignment") // `highlighters[key] = value` doesn't compile.
      highlighters.put(nodeType, mutableListOf(highlighter))
    }
  }
}