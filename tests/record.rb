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

  $verbose = false
  $verout = $stdout

  TestLib.startIJ

  class RecordEventListener
    include AWTEventListener

    private
    @lastMouseXY
    @mouseDragOrigin

    def mouseMoveDirection(x, y)
      dir = [x - @lastMouseXY[0], y - @lastMouseXY[1]]
      @lastMouseXY = [x, y]

      return dir
    end

    public
    def initialize
      super
      @lastMouseXY = nil
      @mouseDragOrigin = nil
    end

    def eventDispatched(event)
      if event.is_a?(ContainerEvent) or
		event.is_a?(HierarchyEvent) or
		event.is_a?(InputMethodEvent) or
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
	$verbose and $verout.puts(Main.getPath(event.getSource))
      elsif event.is_a? ComponentEvent
	if event.is_a? FocusEvent
	  $verbose and $verout.puts(
		       "FocusEvent on component:\n" +
		       "\tevent = #{event.inspect}\n" +
		       "\tsource = #{event.getSource.inspect}"
	  )
	elsif event.is_a? MouseEvent
	  case event.getID

	  # Mouse Button pressed
	  when MouseEvent::MOUSE_PRESSED
	    if @mouseDragOrigin
	      roottitle = nil
	      rootx = @mouseDragOrigin[0].getX
	      rooty = @mouseDragOrigin[0].getY

	      record('mousePress', event.getButton)
	    else
	      co = event.getSource
	      root = co

	      while not (root.is_a?(Dialog) or root.is_a?(Frame))
#	      unless (root.is_a?(Dialog) or root.is_a?(Frame))
		root = root.getParent
	      end

	      @mouseDragOrigin = [root, event.getButton]
	      @lastMouseXY = [event.getXOnScreen, event.getYOnScreen]

	      record('mousePress', event.getButton, root.getTitle,
		     event.getXOnScreen - root.getX,
		     event.getYOnScreen - root.getY)
	    end
	  # Mouse Button released
	  when MouseEvent::MOUSE_RELEASED
	    if @mouseDragOrigin and @mouseDragOrigin[1] == event.getButton
	      @mouseDragOrigin = nil
	    end

	    record('mouseRelease', event.getButton)

	  # Mouse dragged
	  when MouseEvent::MOUSE_DRAGGED
	    if @mouseDragOrigin
	      record('mouseMove', *mouseMoveDirection(event.getXOnScreen, event.getYOnScreen))
	    end
	  end
	end
      elsif $verbose
	$verout.puts "Unknown Event:\n\tevent = #{event.inspect}\n\tsource = #{event.getSource.inspect}"
      end

    end
  end

  Toolkit.getDefaultToolkit.addAWTEventListener(RecordEventListener.new, Long::MAX_VALUE)

  puts "require 'testlib'", '', 'TestLib::startIJ'
end
