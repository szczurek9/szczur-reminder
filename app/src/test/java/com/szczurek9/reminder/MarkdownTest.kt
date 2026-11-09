package com.szczurek9.reminder

import com.szczurek9.reminder.ui.MdBlock
import com.szczurek9.reminder.ui.inlineMarkdown
import com.szczurek9.reminder.ui.markdownToPlain
import com.szczurek9.reminder.ui.parseMarkdownBlocks
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownTest {
    @Test fun `bloki`() {
        val blocks = parseMarkdownBlocks("# Tytuł\n\nlinia 1\nlinia 2\n\n- a\n* b\n1. x\n2) y")
        assertEquals(
            listOf(
                MdBlock.Heading(1, "Tytuł"),
                MdBlock.Paragraph("linia 1\nlinia 2"),
                MdBlock.Bullet("a"),
                MdBlock.Bullet("b"),
                MdBlock.Numbered(1, "x"),
                MdBlock.Numbered(2, "y"),
            ),
            blocks,
        )
    }

    @Test fun `znaczniki inline znikaja z tekstu`() {
        assertEquals("pogrubione kursywa skreslone", inlineMarkdown("**pogrubione** *kursywa* ~~skreslone~~").text)
    }

    @Test fun `niedomkniete znaczniki zostaja literalnie`() {
        assertEquals("2 * 3 = 6", inlineMarkdown("2 * 3 = 6").text)
        assertEquals("**abc", inlineMarkdown("**abc").text)
    }

    @Test fun `bez HTML - tekst bez zmian`() {
        assertEquals("<b>x</b>", inlineMarkdown("<b>x</b>").text)
    }

    @Test fun `tekst zwykly do powiadomien`() {
        assertEquals("Tytuł\n• a\n1. b", markdownToPlain("## **Tytuł**\n- a\n1. b"))
    }
}
