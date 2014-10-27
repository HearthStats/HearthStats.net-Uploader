package net.hearthstats.ui.util

import net.miginfocom.swing.MigLayout

import scala.swing.{Component, LayoutContainer, Panel}

class MigPanel(
                val layoutConstraints: String = "",
                val colConstraints: String = "",
                val rowConstraints: String = "") extends Panel with LayoutContainer {
  override lazy val peer = {
    val mig = new MigLayout(layoutConstraints, colConstraints, rowConstraints)
    new javax.swing.JPanel(mig) with SuperMixin
  }
  private def layoutManager = peer.getLayout.asInstanceOf[MigLayout]


  type Constraints = String

  override def contents: MigContent = new MigContent

  protected class MigContent extends Content {
    def +=(c: Component, l: Constraints) = add(c, l)
  }

  protected def constraintsFor(comp: Component) =
    layoutManager.getComponentConstraints(comp.peer).toString

  protected def areValid(c: Constraints): (Boolean, String) = (true, "")

  protected def add(c: Component, l: Constraints) = peer.add(c.peer, l)

}