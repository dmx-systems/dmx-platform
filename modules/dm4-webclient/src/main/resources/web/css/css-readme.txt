
Note: in contrast to the other bundles the Webclient's stylesheet directory is not named "style" but "css".
This prevents the Webclient stylesheets from being picked up automatically. Auto pick up would be to late.
In order to calculate the canvas size the Webclient style must be present *before* the inital GUI is build.
