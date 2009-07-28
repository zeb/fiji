#!/usr/bin/env ruby
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
include_class 'java.awt.Toolkit'

include_class 'java.awt.event.AWTEventListener'
include_class 'java.awt.event.ActionEvent'
include_class 'java.awt.event.ContainerEvent'
include_class 'java.awt.event.ComponentEvent'
include_class 'java.awt.event.FocusEvent'
include_class 'java.awt.event.HierarchyEvent'
include_class 'java.awt.event.InputMethodEvent'
include_class 'java.awt.event.MouseEvent'
include_class 'java.awt.event.PaintEvent'
include_class 'java.awt.event.WindowEvent'

module Recorder
  def record(function, argument)
    puts "TestLib::#{function}('#{argument}')"
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

  TestLib.startIJ

  allFramesAndDialogs = Hash.new

  listener = AWTEventListener.new
  class << listener
    def eventDispatched(event)
      if event.is_a?(ComponentEvent) or
		event.is_a?(ContainerEvent) or
		event.is_a?(HierarchyEvent) or
		event.is_a?(InputMethodEvent) or
		event.is_a?(MouseEvent) or
		event.is_a?(PaintEvent)
	return
      end

      if event.is_a? ActionEvent
	if event.getSource.is_a? MenuItem
	  getMenuPath(event.getSource)
	elsif event.getSource.is_a? Button
	  getButton(event.getSource)
	elsif $verbose
	  $stderr.puts "Unknown action event: #{event.pretty_inspect}"
	  $stderr.puts "event.getSource = #{event.getSource.pretty_inspect}"
	end
      elsif event.getID == FocusEvent::FOCUS_GAINED and
		event.is_a? Window or
		event.getID == WindowEvent::WINDOW_OPENED
	record('waitForWindow', event.getSource.getTitle)
      elsif $verbose
	$stderr.puts "Unknown action event: #{event.pretty_inspect}"
	$stderr.puts "event.getSource = #{event.getSource.pretty_inspect}"
      end
    end
  end

  Toolkit.getDefaultToolkit.addAWTEventListener(listener, -1)

  puts "require 'testlib'", '', 'TestLib::startIJ'
end
