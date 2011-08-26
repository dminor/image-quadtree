/*
Copyright (c) 2011 Daniel Minor 

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

import java.awt.Color
import java.awt.Point
import java.awt.Rectangle
import java.awt.image._
import java.math._

package quadtree {

    class Quadtree(img : BufferedImage, numBands : Int, minLevel : Int) {

        class Node(val mid : Point, val radius : Int) { 
            var nw : Node = null
            var ne : Node = null
            var sw : Node = null
            var se : Node = null
            var data : Int = 0 

            def isLeaf() : Boolean = {
                nw == null && ne == null && sw == null && se == null
            } 
        }

        val maxlevel = ((math.log(math.max(img.getHeight, img.getWidth))/math.log(2.0)) ceil) toInt

        var root : Node = construct(maxlevel, img.getWidth - 1,
            img.getHeight - 1, img.getData)._1

        def construct(level : Int, x : Int, y : Int, data : Raster) : (Node, Int) = { 

            if (level == 0) {
                //just return pixel value 
                if (x < 0 || x >= img.getWidth || y < 0 || y >= img.getHeight) {
                    return (null, 0)
                } else {
                    val pixel : Array[Int] = data.getPixel(x, y, null)
                    return (null, pixel(0)/(256/numBands))
                }
            } else {
                val next_level = level - 1
                val delta = 1 << next_level

                val nw = construct(next_level, x - delta, y - delta, data)
                val ne = construct(next_level, x, y - delta, data)
                val sw = construct(next_level, x - delta, y, data)
                val se = construct(next_level, x, y, data) 

                if (level < minLevel) {
                    //return median of four values
                    val median = List(nw._2, ne._2, sw._2, se._2).sortWith(_ < _)(2)
                    return (null, median)
                } else if (nw._2 != 255 && nw._2 == ne._2 &&
                    nw._2 == sw._2 && nw._2 == se._2) { 
                    return (null, nw._2)
                } else {
                    val radius = 1 << (level - 1)
                    val mid_x = x - radius
                    val mid_y = y - radius

                    val node = new Node(new Point(mid_x, mid_y), radius)
                    if (nw._1 == null) {
                        node.nw = new Node(new Point(mid_x - radius/2,
                            mid_y - radius/2), radius/2)
                        node.nw.data = nw._2 
                    } else { 
                        node.nw = nw._1
                    }

                    if (ne._1 == null) {
                        node.ne = new Node(new Point(mid_x + radius/2,
                            mid_y-radius/2), radius/2)
                        node.ne.data = ne._2 
                    } else { 
                        node.ne = ne._1
                    }

                    if (sw._1 == null) {
                        node.sw = new Node(new Point(mid_x - radius/2,
                            mid_y + radius/2), radius/2)
                        node.sw.data = sw._2 
                    } else { 
                        node.sw = sw._1
                    } 

                    if (se._1 == null) {
                        node.se = new Node(new Point(mid_x + radius/2,
                            mid_y + radius/2), radius/2)
                        node.se.data = se._2 
                    } else { 
                        node.se = se._1
                    } 

                    return (node, 255) 
                }
            } 
        }

        def render(img : BufferedImage) = {

            val g = img.createGraphics()

            def worker(n : Node) : Unit = {
                if (n != null && n.radius > 0) {

                    if (n.isLeaf) {
                        val colour = n.data*256/numBands
                        g.setColor(new Color(colour, colour, colour))
                        g.fillRect(n.mid.x-n.radius,n.mid.y-n.radius,
                            2*n.radius, 2*n.radius)
                        g.setColor(Color.WHITE)
                        g.drawRect(n.mid.x-n.radius,n.mid.y-n.radius,
                            2*n.radius, 2*n.radius) 
                    }

                    worker(n.nw)
                    worker(n.ne)
                    worker(n.sw)
                    worker(n.se) 
                }
            }

            worker(root) 
        } 
    } 
}
