#!/usr/bin/env fiji

require 'java'
require 'testlib'

include_class 'java.awt.Toolkit'
include_class 'java.awt.event.AWTEventListener'
include_class 'java.awt.event.WindowEvent'

class DialogDumpListener
  include AWTEventListener

  def eventDispatched(event)
    if event.getID == WindowEvent::WINDOW_OPENED
      puts "--- BEGIN DUMP OF WINDOW \"#{event.getSource.getTitle}\" ---"
      dumpWindowLayout(event.getSource)
      puts "---- END DUMP OF WINDOW \"#{event.getSource.getTitle}\" ----", ''
    end
  end

  private
  def dumpWindowLayout(container, cd=0)
    puts "#{'  ' * cd} #{container.class}"
    if container.respond_to? :getComponents
      container.getComponents.each { |x| dumpWindowLayout(x, cd + 1) }
    end
  end
end

if __FILE__ == $0
  Toolkit.getDefaultToolkit.addAWTEventListener(DialogDumpListener.new, -1)
  TestLib.startIJ
end
