#!/usr/bin/env fiji
#

require 'java'
require 'testlib'

require 'pp'

include_class 'java.awt.AWTEvent'
include_class 'java.awt.Button'
include_class 'java.awt.Dialog'
include_class 'java.awt.Frame'
include_class 'java.awt.Menu'
include_class 'java.awt.MenuBar'
include_class 'java.awt.MenuItem'
include_class 'java.awt.TextField'
include_class 'java.awt.Toolkit'
include_class 'java.awt.Window'

include_class 'java.awt.event.AWTEventListener'
include_class 'java.awt.event.ActionEvent'
include_class 'java.awt.event.ContainerEvent'
include_class 'java.awt.event.ComponentEvent'
include_class 'java.awt.event.FocusEvent'
include_class 'java.awt.event.HierarchyEvent'
include_class 'java.awt.event.InputMethodEvent'
include_class 'java.awt.event.MouseEvent'
include_class 'java.awt.event.PaintEvent'
include_class 'java.awt.event.TextEvent'
include_class 'java.awt.event.WindowEvent'

include_class 'javax.swing.JTextField'

include_class 'java.lang.Long'

module Recorder
  def record(function, *args)
    puts "TestLib::#{function}(#{args.collect { |x| x.inspect }.join(', ')})"
  end

  def getMenuPath(menuItem)
    result = ''
    until menuItem.is_a?(MenuBar)
      result = ">#{result}" unless result == ''
      result = menuItem.getLabel + result
      menuItem = menuItem.getParent
    end

    if menuItem.getParent.is_a?(Frame)
      record('waitForWindow', menuItem.getParent.getTitle)
    end

    record('clickMenuItem', result)
  end

  def getButton(button)
    label = button.getLabel
    while button
      if button.is_a?(Frame) or button.is_a?(Dialog)
	record('waitForWindow', button.getTitle)
	break
      end

      button = button.getParent
    end

    record('clickButton', label)
  end
end

if __FILE__ == $0
  include Recorder

  $verbose = true
  $verout = $stdout

  TestLib.startIJ

  listener = AWTEventListener.new
  class << listener
    def eventDispatched(event)
      if event.is_a?(ContainerEvent) or
		event.is_a?(HierarchyEvent) or
		event.is_a?(InputMethodEvent) or
		event.is_a?(MouseEvent) or
		event.is_a?(PaintEvent) or
		event.is_a?(TextEvent)
	return
      end

      if event.is_a? ActionEvent
	if event.getSource.is_a? MenuItem
	  getMenuPath(event.getSource)
	elsif event.getSource.is_a? Button
	  getButton(event.getSource)
	elsif $verbose
	  $verout.puts "Unknown ActionEvent:\n\tevent = #{event.inspect}\n\tsource = #{event.getSource.inspect}"
	end
      elsif event.getID == FocusEvent::FOCUS_GAINED and
		event.getSource.is_a? Window or
		event.getID == WindowEvent::WINDOW_OPENED
	record('waitForWindow', event.getSource.getTitle)
      elsif event.getID == FocusEvent::FOCUS_GAINED
	$verout.puts(getComponentPath(event.getSource))
      elsif event.is_a? ComponentEvent
	if event.is_a? FocusEvent
	  $verout.puts("FocusEvent on component:\n\tevent = #{event.inspect}\n\tsource = #{event.getSource.inspect}")
	end
      elsif $verbose
	$verout.puts "Unknown Event:\n\tevent = #{event.inspect}\n\tsource = #{event.getSource.inspect}"
      end

    end
  end

  Toolkit.getDefaultToolkit.addAWTEventListener(listener, Long::MAX_VALUE)

  puts "require 'testlib'", '', 'TestLib::startIJ'
end
