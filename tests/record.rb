#!/usr/bin/env ruby
#

require 'java'
require 'testlib'

['AWTEvent', 'Button', 'Dialog', 'Frame', 'Menu', 'MenuBar', 'MenuItem', 'Toolkit'].each do |x|
  include_class "java.awt.#{x}"
end

['AWTEventListener', 'ActionEvent', 'ContainerEvent', 'ComponentEvent', 'FocusEvent',
  'HierarchyEvent', 'InputMethodEvent', 'MouseEvent', 'PaintEvent', 'WindowEvent'].each do |x|
  include_class "java.awt.event.#{x}"
end

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
      record('waitForFrame', menuItem.getParent.getTitle)
    end

    record('clickMenuItem', result)
  end

  def getButton(button)
    label = button.getLabel
    while button
      if button.is_a?(Frame) or button.is_a?(Dialog)
	record('waitForFrame', button.getTitle)
	break
      end

      button = button.getParent
    end

    record('clickButton', label)
  end
end

if __FILE__ == $0
  include Recorder

  TestLib.startIJ

  allFramesAndDialogs = Hash.new

  listener = AWTEventListener.new
  class << listener
    def eventDispatched(event)
      return if [ComponentEvent, ContainerEvent, HierarchyEvent,
	InputMethodEvent, MouseEvent, PaintEvent].find { |c|
	  event.is_a? c }

      if event.is_a? ActionEvent
	if event.getSource.is_a? MenuItem
	  getMenuPath(event.getSource)
	elsif event.getSource.is_a? Button
	  getButton(event.getSource)
	else
	  puts "Unknown event: #{event}"
	end
      elsif event.getID == FocusEvent::FOCUS_GAINED and \
	  (event.is_a? Frame or event.is_a? Dialog)
	unless allFramesAndDialogs.has? event.getSource
	  allFramesAndDialogs[event.getSource] = true
	  function = event.getSource.is_a?(Dialog) ?
	    'waitForDialog' : 'waitForFrame'
	  record(function, event.getSource.getTitle)
	end
      elsif event.getID == WindowEvent::WINDOW_CLOSED
	allFramesAndDialogs.delete event.getSource
      else
	puts "event #{event} from source #{event.getSource}"
      end
    end
  end

  Toolkit.getDefaultToolkit.addAWTEventListener(listener, -1)

  puts "require 'testlib'", '', 'TestLib::startIJ'
end
