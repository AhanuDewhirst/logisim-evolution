/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;
import com.cburch.logisim.util.ContentBuilder;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class CounterHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NrOfBitsStr = "width";
  private static final int NrOfBitsId = -1;
  private static final String MaxValStr = "max_val";
  private static final int MaxValId = -2;
  private static final String activeEdgeStr = "ClkEdge";
  private static final int ActiveEdgeId = -3;
  private static final String modeStr = "mode";
  private static final int ModeId = -4;

  @Override
  public String getComponentStringIdentifier() {
    return "COUNTER";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("GlobalClock", 1);
    map.put("ClockEnable", 1);
    map.put("LoadData", NrOfBitsId);
    map.put("clear", 1);
    map.put("load", 1);
    map.put("Up_n_Down", 1);
    map.put("Enable", 1);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = new ContentBuilder();
    contents.addRemarkBlock(
        "Functionality of the counter:\\ __Load_Count_|_mode\\ ____0____0___|_halt\\ "
            + "____0____1___|_count_up_(default)\\ ____1____0___|load\\ ____1____1___|_count_down");
    if (HDL.isVHDL()) {
      contents
          .add("   CompareOut   <= s_carry;")
          .add("   CountValue   <= s_counter_value;")
          .add("")
          .add("   make_carry : PROCESS( Up_n_Down,")
          .add("                         s_counter_value )")
          .add("   BEGIN")
          .add("      IF (Up_n_Down = '0') THEN")
          .add("         IF (s_counter_value = std_logic_vector(to_unsigned(0,width))) THEN")
          .add("            s_carry <= '1';")
          .add("         ELSE")
          .add("            s_carry <= '0';")
          .add("         END IF; -- Down counting")
          .add("      ELSE")
          .add("")
          .add("         IF (s_counter_value = std_logic_vector(to_unsigned(max_val,width))) THEN")
          .add("            s_carry <= '1';")
          .add("         ELSE")
          .add("            s_carry <= '0';")
          .add("         END IF; -- Up counting")
          .add("      END IF;")
          .add("   END PROCESS make_carry;")
          .add("")
          .add("   s_real_enable <= '0' WHEN (load = '0' AND enable = '0') -- Counter disabled")
          .add("                          OR (mode = 1 AND s_carry = '1' AND load = '0') -- Stay at value situation")
          .add("                        ELSE ClockEnable;")
          .add("")
          .add("   make_next_value : PROCESS( load , Up_n_Down , s_counter_value ,")
          .add("                              LoadData , s_carry )")
          .add("      VARIABLE v_downcount : std_logic;")
          .add("   BEGIN")
          .add("      v_downcount := NOT(Up_n_Down);")
          .add("      IF ((load = '1') OR -- load condition")
          .add("          (mode = 3 AND s_carry = '1')    -- Wrap load condition")
          .add("         ) THEN s_next_counter_value <= LoadData;")
          .add("      ELSE")
          .add("         CASE (mode) IS")
          .add("            WHEN  0     => IF (s_carry = '1') THEN")
          .add("                              IF (v_downcount = '1') THEN")
          .add("                                 s_next_counter_value <= std_logic_vector(to_unsigned(max_val,width));")
          .add("                              ELSE")
          .add("                                 s_next_counter_value <= (OTHERS => '0');")
          .add("                              END IF;")
          .add("                           ELSE")
          .add("                              IF (v_downcount = '1') THEN")
          .add("                                 s_next_counter_value <= std_logic_vector(unsigned(s_counter_value) - 1);")
          .add("                              ELSE")
          .add("                                 s_next_counter_value <= std_logic_vector(unsigned(s_counter_value) + 1);")
          .add("                              END IF;")
          .add("                           END IF;")
          .add("            WHEN OTHERS => IF (v_downcount = '1') THEN")
          .add("                              s_next_counter_value <= std_logic_vector(unsigned(s_counter_value) - 1);")
          .add("                           ELSE")
          .add("                              s_next_counter_value <= std_logic_vector(unsigned(s_counter_value) + 1);")
          .add("                           END IF;")
          .add("         END CASE;")
          .add("      END IF;")
          .add("   END PROCESS make_next_value;")
          .add("")
          .add("   make_flops : PROCESS( GlobalClock , s_real_enable , clear , s_next_counter_value )")
          .add("      VARIABLE temp : std_logic_vector(0 DOWNTO 0);")
          .add("   BEGIN")
          .add("      temp := std_logic_vector(to_unsigned(%s, 1));", activeEdgeStr)
          .add("      IF (clear = '1') THEN s_counter_value <= (OTHERS => '0');")
          .add("      ELSIF (GlobalClock'event AND (GlobalClock = temp(0))) THEN")
          .add("         IF (s_real_enable = '1') THEN s_counter_value <= s_next_counter_value;")
          .add("         END IF;")
          .add("      END IF;")
          .add("   END PROCESS make_flops;");
    } else {
      contents
          .add("")
          .add("   assign CompareOut = s_carry;")
          .add("   assign CountValue = (%s) ? s_counter_value : s_counter_value_neg_edge;", activeEdgeStr)
          .add("")
          .add("   always@(*)")
          .add("   begin")
          .add("      if (Up_n_Down)")
          .add("         begin")
          .add("            if (%s)", activeEdgeStr)
          .add("               s_carry = (s_counter_value == max_val) ? 1'b1 : 1'b0;")
          .add("            else")
          .add("               s_carry = (s_counter_value_neg_edge == max_val) ? 1'b1 : 1'b0;")
          .add("         end")
          .add("      else")
          .add("         begin")
          .add("            if (%s)", activeEdgeStr)
          .add("               s_carry = (s_counter_value == 0) ? 1'b1 : 1'b0;")
          .add("            else")
          .add("               s_carry = (s_counter_value_neg_edge == 0) ? 1'b1 : 1'b0;")
          .add("         end")
          .add("   end")
          .add("")
          .add("   assign s_real_enable = ((~(load)&~(Enable))|")
          .add("                           ((mode==1)&s_carry&~(load))) ? 1'b0 : ClockEnable;")
          .add("")
          .add("   always @(*)")
          .add("   begin")
          .add("      if ((load)|((mode==3)&s_carry))")
          .add("         s_next_counter_value = LoadData;")
          .add("      else if ((mode==0)&s_carry&Up_n_Down)")
          .add("         s_next_counter_value = 0;")
          .add("      else if ((mode==0)&s_carry)")
          .add("         s_next_counter_value = max_val;")
          .add("      else if (Up_n_Down)")
          .add("         begin")
          .add("            if (%s)", activeEdgeStr)
          .add("               s_next_counter_value = s_counter_value + 1;")
          .add("            else")
          .add("               s_next_counter_value = s_counter_value_neg_edge + 1;")
          .add("         end")
          .add("      else")
          .add("         begin")
          .add("            if (%s)", activeEdgeStr)
          .add("               s_next_counter_value = s_counter_value - 1;")
          .add("            else")
          .add("               s_next_counter_value = s_counter_value_neg_edge - 1;")
          .add("         end")
          .add("   end")
          .add("")
          .add("   always @(posedge GlobalClock or posedge clear)")
          .add("   begin")
          .add("       if (clear) s_counter_value <= 0;")
          .add("       else if (s_real_enable) s_counter_value <= s_next_counter_value;")
          .add("   end")
          .add("")
          .add("   always @(negedge GlobalClock or posedge clear)")
          .add("   begin")
          .add("       if (clear) s_counter_value_neg_edge <= 0;")
          .add("       else if (s_real_enable) s_counter_value_neg_edge <= s_next_counter_value;")
          .add("   end")
          .add("");
    }
    return contents.get();
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist nets, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("CountValue", NrOfBitsId);
    map.put("CompareOut", 1);
    return map;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    final var map = new TreeMap<Integer, String>();
    map.put(NrOfBitsId, NrOfBitsStr);
    map.put(MaxValId, MaxValStr);
    map.put(ActiveEdgeId, activeEdgeStr);
    map.put(ModeId, modeStr);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist nets, NetlistComponent componentInfo) {
    final var map = new TreeMap<String, Integer>();
    final var attrs = componentInfo.GetComponent().getAttributeSet();
    var mode = 0;
    if (attrs.containsAttribute(Counter.ATTR_ON_GOAL)) {
      if (attrs.getValue(Counter.ATTR_ON_GOAL) == Counter.ON_GOAL_STAY) mode = 1;
      else if (attrs.getValue(Counter.ATTR_ON_GOAL) == Counter.ON_GOAL_CONT) mode = 2;
      else if (attrs.getValue(Counter.ATTR_ON_GOAL) == Counter.ON_GOAL_LOAD) mode = 3;
    } else {
      mode = 1;
    }
    map.put(NrOfBitsStr, attrs.getValue(StdAttr.WIDTH).getWidth());
    map.put(MaxValStr, attrs.getValue(Counter.ATTR_MAX).intValue());
    var clkEdge = 1;
    if (GetClockNetName(componentInfo, Counter.CK, nets).isEmpty()
        && attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING) clkEdge = 0;
    map.put(activeEdgeStr, clkEdge);
    map.put(modeStr, mode);
    return map;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var componentInfo = (NetlistComponent) mapInfo;
    final var attrs = componentInfo.GetComponent().getAttributeSet();
    if (!componentInfo.EndIsConnected(Counter.CK)) {
      Reporter.Report.AddSevereWarning(
          "Component \"Counter\" in circuit \""
              + nets.getCircuitName()
              + "\" has no clock connection");
      map.put("GlobalClock", HDL.zeroBit());
      map.put("ClockEnable", HDL.zeroBit());
    } else {
      final var clockNetName = GetClockNetName(componentInfo, Counter.CK, nets);
      if (clockNetName.isEmpty()) {
        map.putAll(GetNetMap("GlobalClock", true, componentInfo, Counter.CK, nets));
        map.put("ClockEnable", HDL.oneBit());
      } else {
        var clockBusIndex = ClockHDLGeneratorFactory.DerivedClockIndex;
        if (nets.RequiresGlobalClockConnection()) {
          clockBusIndex = ClockHDLGeneratorFactory.GlobalClockIndex;
        } else {
          if (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_LOW)
            clockBusIndex = ClockHDLGeneratorFactory.InvertedDerivedClockIndex;
          else if (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_RISING)
            clockBusIndex = ClockHDLGeneratorFactory.PositiveEdgeTickIndex;
          else if (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING)
            clockBusIndex = ClockHDLGeneratorFactory.InvertedDerivedClockIndex;
        }
        map.put(
            "GlobalClock",
            clockNetName
                + HDL.BracketOpen()
                + ClockHDLGeneratorFactory.GlobalClockIndex
                + HDL.BracketClose());
        map.put(
            "ClockEnable",
            clockNetName + HDL.BracketOpen() + clockBusIndex + HDL.BracketClose());
      }
    }
    var input = "LoadData";
    if (HDL.isVHDL() & (componentInfo.GetComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth() == 1))
      input += "(0)";
    map.putAll(GetNetMap(input, true, componentInfo, Counter.IN, nets));
    map.putAll(GetNetMap("clear", true, componentInfo, Counter.CLR, nets));
    map.putAll(GetNetMap("load", true, componentInfo, Counter.LD, nets));
    map.putAll(GetNetMap("Enable", false, componentInfo, Counter.EN, nets));
    map.putAll(
        GetNetMap("Up_n_Down", false, componentInfo, Counter.UD, nets));
    var output = "CountValue";
    if (HDL.isVHDL() & (componentInfo.GetComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth() == 1))
      output += "(0)";
    map.putAll(GetNetMap(output, true, componentInfo, Counter.OUT, nets));
    map.putAll(GetNetMap("CompareOut", true, componentInfo, Counter.CARRY, nets));
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetRegList(AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("s_next_counter_value", NrOfBitsId); // for verilog generation
    // in explicite process
    map.put("s_carry", 1); // for verilog generation in explicite process
    map.put("s_counter_value", NrOfBitsId);
    if (HDL.isVerilog()) map.put("s_counter_value_neg_edge", NrOfBitsId);
    return map;
  }

  @Override
  public String GetSubDir() {
    /*
     * this method returns the module directory where the HDL code needs to
     * be placed
     */
    return "memory";
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var map = new TreeMap<String, Integer>();
    map.put("s_real_enable", 1);
    return map;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
