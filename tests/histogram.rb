#!/usr/bin/env fiji

require 'java'
require 'testlib'

include_class 'java.lang.System'
include_class 'java.awt.Rectangle'
include_class 'java.awt.Robot'
include_class 'java.awt.Toolkit'
include_class 'java.awt.Window'
include_class 'java.awt.event.AWTEventListener'
include_class 'java.awt.event.FocusEvent'
include_class 'java.awt.event.KeyEvent'
include_class 'java.awt.event.WindowEvent'

include_class 'ij.ImagePlus'

class HistogramListener
  include AWTEventListener

  def initialize
    @shiftCount = 0
    @shiftStartTime = 0
    @currentComponent = nil
  end

  def eventDispatched(event)
    unless event.is_a? KeyEvent or
	   event.is_a? MouseEvent
      return
    end

    if event.getID == MouseEvent::MOUSE_MOVED
      @currentComponent = event.getSource or currentComponent
    elsif event.getID == KeyEvent::KEY_PRESSED and
	event.getKeyCode == KeyEvent::VK_SHIFT

      puts event.getSource.inspect, @currentComponent.inspect

      if @shiftCount == 0 or System.currentTimeMillis - @shiftStartTime > 1000
	@shiftStartTime = System.currentTimeMillis
	@shiftCount = 1
      elsif @shiftCount == 2
	@shiftCount = 0

	componentLocation = @currentComponent.getLocationOnScreen
	componentSize     = @currentComponent.getSize

	rectangle = Rectangle.new(componentLocation, componentSize)

	robot = Robot.new
	image = robot.createScreenCapture(rectangle)

	ijplus = ImagePlus.new('ScreenShot', image)

	ijplus.show

	rgb = ijplus.getProcessor.getPixels

	pixels = {
	  "red"   => Array.new(256) { 0 },
	  "green" => Array.new(256) { 0 },
	  "blue"  => Array.new(256) { 0 },
	}

	rgb.each_with_index do |pixel,idx|
	  if idx % 100 == 0
	    printf("\rCalculating histogram... %d%%", (idx+1).to_f/rgb.size * 100)
	  end
	  pixels["red"][pixel >> 16 & 0xff]  += 1
	  pixels["green"][pixel >> 8 & 0xff] += 1
	  pixels["blue"][pixel & 0xff]       += 1
	end

	puts("\rCalculating histogram... done")

	pixels.each_key do |color|
	  puts("Color %s: mu = %.02f, var = %.02f" % [color, *moments(pixels[color])])
	end

      else
	@shiftCount += 1
      end
    end
  end

  private
  def moments(array)
    total = 0
    mu  = 0
    var = 0
    array.each_with_index do |v,i|
      total += v
      mu    += v * i
      var   += v * i * i
    end

    mu  /= total.to_f
    var /= total.to_f

    var -= mu * mu

    return mu, Math::sqrt(var)
  end
end

if __FILE__ == $0
  Toolkit.getDefaultToolkit.addAWTEventListener(HistogramListener.new, -1)
  TestLib.startIJ
end
