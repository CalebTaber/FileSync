import gi

gi.require_version("Gtk", "4.0")
from gi.repository import Gtk

WINDOW_WIDTH = 600
WINDOW_HEIGHT = 600


def on_activate(app):
    win = Gtk.ApplicationWindow(application=app, title='FileSync', default_width=WINDOW_WIDTH, default_height=WINDOW_HEIGHT)
    layout = Gtk.FlowBox()
    win.set_child(layout)
    win.present()


app = Gtk.Application()
app.connect('activate', on_activate)

# Run the application
app.run(None)
