package intellij.impl

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class TestAction : AnAction() {

  override fun actionPerformed(p0: AnActionEvent) {
    Messages.showInfoMessage("Hello World", "Greetings")
  }
}