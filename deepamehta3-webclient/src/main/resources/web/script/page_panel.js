function PagePanel() {
    
    var dom = $("<div>").attr("id", "page-panel")
    dom.append($("<div>").attr("id", "page-content"))
    dom.append($("<div>").attr("id", "page-toolbar").addClass("dm-toolbar"))

    this.dom = dom

    // -------------------------------------------------------------------------------------------------- Public Methods

    this.empty = function() {
        $("#page-content").empty()
        $("#page-toolbar").empty()
    }
}
