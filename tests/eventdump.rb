#!/usr/bin/env fiji

require 'java'
require 'testlib'
require 'pp'

include_class 'java.awt.Toolkit'
include_class 'java.awt.event.AWTEventListener'
include_class 'java.awt.event.WindowEvent'

class EventDumpListener
  include AWTEventListener

  def eventDispatched(event)
    puts "===== #{event.class} received ====="
    puts "  event: #{event.pretty_inspect}"
    puts "  source: #{event.getSource.pretty_inspect}"
    puts("=" * (event.class.to_s.size + 21))
    puts
  end
end

if __FILE__ == $0
  Toolkit.getDefaultToolkit.addAWTEventListener(EventDumpListener.new, -1)
  TestLib.startIJ
end
