import gi

gi.require_version("Gtk", "4.0")
from gi.repository import Gtk

WINDOW_WIDTH = 500
WINDOW_HEIGHT = 400


def on_activate(app):
    win = Gtk.ApplicationWindow(application=app, title='File Synchronizer')
    layout = Gtk.FlowBox()
    win.set_child(layout)
    win.present()


app = Gtk.Application()
app.connect('activate', on_activate)

# Run the application
app.run(None)
