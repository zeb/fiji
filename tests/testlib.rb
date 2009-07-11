#!/usr/bin/env ruby
#

# TODO
#   replace obj.getFoo with obj.foo

require 'java'

include_class 'fiji.Main'

['IJ', 'ImageJ', 'WindowManager'].each do |x|
  include_class "ij.#{x}"
end

['Button', 'Container', 'Dialog', 'Frame', 'Toolkit'].each do |x|
  include_class "java.awt.#{x}"
end

['AWTEventListener', 'ActionEvent', 'ContainerEvent', 'FocusEvent',
    'HierarchyEvent', 'MouseEvent'].each do |x|
  include_class "java.awt.event.#{x}"
end


module TestLib
  @currentFrame
  @currentDialog
  puts @currentFrame.object_id

  def self.startIJ
    Main.premain
    @currentFrame = ImageJ.new
    @currentFrame.exitWhenQuitting(true)
    Main.postmain
  end

  def catchIJErrors(&block)
    begin
      IJ.redirectErrorMessage
      return yield
    rescue
      logWindow = WindowManager.getFrame('Log')
      if logWindow
	error_message = logWindow.getTextPanel.getText
	logWindow.close
	return error_message
      end
    end
  end

  def test(&block)
    if catchIJErrors(&block)
      print "Failed: " + block.to_s
      exit 1
    end
  end

  def waitForFrame(title)
    cf = Frame.getFrames.find { |frame| frame.getTitle == title }

    if cf
      return @currentFrame = cf
    end

    listener = AWTEventListener.new

    class << listener
      attr_reader :lock
      @lock = Mutex.new
      @lock.lock
      puts @currentFrame.object_id

      def eventDispatched(event)
	if event.getID != FocusEvent::FOCUS_GAINED and \
	    event.getID != HierarchyEvent::DISPLAYABILITY_CHANGED
	  return
	end

	source = event.getSource
	if source.is_a?(Frame) and source.getTitle == title
	  @currentFrame = source
	  @lock.unlock
	end
      end
    end

    Toolkit.getDefaultToolkit.addAWTEventListener(listener, -1)
    listener.lock.lock
    listener.lock.unlock
    Toolkit.getDefaultToolkit.removeAWTEventListener(listener)
    return @currentFrame
  end

  def waitForDialog(title)
    cd = Frame.getFrames.find do |frame|
      frame.getOwnedWindows.find do |window|
	window.is_a? Dialog and window.getTitle == title
      end
    end

    return @currentDialog = cd if cd

    listener = AWTEventListener.new

    class << listener
      attr_reader :lock
      @lock = Mutex.new
      @lock.lock

      def eventDispatched(event)
	source = event.getSource
	if souce.is_a? Dialog and source.title == title
	  return if event.getID == ContainerEvent::COMPONENT_ADDED
	  TestLib::currentDialog = source
	  @lock.unlock
	end
      end
    end

    Toolkit.getDefaultToolkit.addAWTEventListener(listener, -1)
    listener.lock.lock
    listener.lock.unlock
    Toolkit.getDefaultToolkit.removeAWTEventListener(listener)
    return @currentDialog
  end

  def getMenuEntry(menuBar, path)
    menuBar ||= @currentFrame.getMenuBar 
    path = path.split('>') if path.is_a? String

    begin
      menu = nil
      menuBar.getMenuCount.times do |i|
	menu = menuBar.getMenu(i) if  path[0] == menuBar.getMenu(i).getLabel
      end

      path[1..-1].each do |label|
	entry = nil
	menu.getItemCount.times do |i|
	  entry = menu.getItem(i) if label == menu.getItem(i).getLabel
	  break
	end
	menu = entry
      end

      return menu
    rescue
      return nil
    end
  end

  def dispatchActionEvent(component)
    event = ActionEvent.new(component, ActionEvent::ACTION_PERFORMED, 
			    component.getLabel, MouseEvent::BUTTON1)
    component.dispatchEvent(event)
  end

  def clickMenuItem(path)
    menuEntry = getMenuEntry(nil, path)
    dispatchActionEvent(menuEntry)
  end

  def getButton(container, label)
    container ||= @currentDialog
    container.getComponents.each do |co|
      if co.is_a? Container
	return result if result = getButton(co, label)
      elsif co.is_a? Button and co.getLabel == label
	return co
      end
    end

    return nil
  end

  def clickButton(label)
     button = getButton(nil, label)
     dispatchActionEvent(button)
  end

  def self.quitIJ
    IJ.getInstance.quit
    @currentFrame = nil
  end
end
