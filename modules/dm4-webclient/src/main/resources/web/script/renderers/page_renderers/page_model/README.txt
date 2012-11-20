The page_model.js utility must not be located in the page_renderers folder directly.
Otherwise it would be considered a renderer and the Webclient would wait for its registration forever.
