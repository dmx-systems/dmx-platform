
CKEditor Update Instructions
============================

DeepaMehta uses a CKEditor custom skin "kama_dm".
This is based on the "kama" skin of CKEditor 3.6.2

Perform these steps when updating CKEditor:

1) Move the original "kama" skin folder to /src/main/resources/web/style/ckeditor_skin
   and rename it to "kama_dm"

2) In 4 files globally replace "kama" by "kama_dm":

    skin.js
    dialog.css
    editor.css
    templates.css

3) In editor.css:

    3a) Purpose: remove a) radius, b) border, c) padding
        Original (CKEditor 3.6.2) rule:

        span.cke_skin_kama_dm {                 <= Remove entire rule
            -moz-border-radius: 5px;
            -webkit-border-radius: 5px;
            border-radius: 5px;
            border: 1px solid #D3D3D3;
            padding: 5px;
        }

    3b) Purpose: remove radius
        Original (CKEditor 3.6.2) rule:

        .cke_skin_kama_dm .cke_wrapper {
            -moz-border-radius: 5px;            <= Remove this
            -webkit-border-radius: 5px;         <= Remove this
            -webkit-touch-callout: none;
            border-radius: 5px;                 <= Remove this
            background-color: #d3d3d3;
            background-image: url(images/sprites.png);
            background-repeat: repeat-x;
            background-position: 0 -1950px;
            display: block;
            _display: inline-block;
            padding: 5px;
        }
