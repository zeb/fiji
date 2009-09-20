#!/usr/bin/env fiji
#

require 'java'

include_class 'ij.IJ'

IJ.newImage('fake-neuron', 'black', 320, 320, 64)
image = IJ.getImage

$pixels = []

image.getNSlices.times do |num|
  image.setSlice(num+1)
  $pixels[num] = image.getProcessor.getPixels
end

def set_pixel(x, y, z, value)
  $pixels[z][x + y * 320] = value
end

x, y, z = rand(320), rand(320), rand(64)

(x-10..x+10).each do |x1|
  next if x1 < 0 || x1 > 319
  (y-10..y+10).each do |y1|
    next if y1 < 0 || y1 > 319
    (z-10..z+10).each do |z1|
      next if z1 < 0 || z1 > 63
      dist = Math.sqrt((x - x1)**2 + (y - y1)**2 + (z - z1)**2)
      set_pixel(x1, y1, z1, 255) unless dist > 10.0
    end
  end
end

image.updateAndDraw
