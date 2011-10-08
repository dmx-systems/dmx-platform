function SplitPanel() {

    var left_panel  = $("<td>").append($("<div>", {id: "canvas-panel"}))
    var right_panel = $("<td>")

    this.dom = $("<table>", {id: "split-panel"}).append($("<tr>")
        .append(left_panel)
        .append(right_panel)
    )

    this.set_left_panel = function(panel) {
        $("#canvas").remove()               // FIXME: don't rely on ID #canvas
        $("#canvas-panel").append(panel)
    }

    this.set_right_panel = function(panel) {
        right_panel.empty()
        right_panel.append(panel)
    }
}
