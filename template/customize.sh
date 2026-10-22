SKIPUNZIP=0

ui_print "****************************************"
ui_print "*               ReGyro                 *"
ui_print "*    Counter Flame Gyroscope Fix       *"
ui_print "*          Zygisk Edition              *"
ui_print "*     Author: dinhmaiphuong2025        *"
ui_print "****************************************"

if [ "$ARCH" != "arm64" ] && [ "$ARCH" != "arm" ]; then
  abort "! Unsupported architecture: $ARCH (ReGyro requires ARM / ARM64)"
fi

ui_print "- Installing ReGyro Zygisk module..."
