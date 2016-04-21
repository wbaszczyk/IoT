server_ip = "coap://192.168.43.1:5683/"

cc = coap.Client()		
-- wait for ip and register this device
tmr.alarm(1, 1000, 1, function()
	if wifi.sta.getip()== nil then
		print("IP unavaiable, Waiting...")
	else
		tmr.stop(1)

		register = {title = "Device", ip = wifi.sta.getip(), port = 5683, mac = wifi.ap.getmac(), version = 1}
		ok, json = pcall(cjson.encode, register)
		cc:post(coap.CON, server_ip .. "register", json)
 	end
end)

cs=coap.Server()
cs:listen(5683)

-- function should take only one string, return one string.
function uuid(payload)
	print(payload)
	UUID=payload
	tmr.start(2)
	respond = "OK"
	return respond
end
cs:func("uuid")

tmr.register(2, 1000, 1, function()
	if UUID == nil then
		print("UUID unavaiable, Waiting...")
	else
		touch_to_json = {uuid = UUID, proximity = touch_val}
		ok, touch_json = pcall(cjson.encode, touch_to_json)
		cc:post(coap.CON, server_ip .. "proximity", touch_json)
	end
end)

-- LED
R=2
G=8
B=1
pwm.setup(R, 100, 1)
pwm.setup(G, 100, 1)
pwm.setup(B, 100, 1)



-- --  ====================================================

-- --  ====================================================

id=0
sda=6
scl=7

i2c.setup(id,sda,scl,i2c.SLOW)

function read_reg(dev_addr, reg_addr)
	i2c.start(id)
	i2c.write(id,dev_addr)
	i2c.write(id,reg_addr)
	i2c.start(id)
	i2c.write(id,0x51)
	c=i2c.read(id,1)
	i2c.stop(id)
	return c
end

function write_reg(dev_addr, reg_addr, reg_data)
	i2c.start(id)
	i2c.write(id,dev_addr)
	i2c.write(id,reg_addr)
	i2c.write(id,reg_data)
	i2c.stop(id)
end


-- --  ====================================================

-- --  ====================================================

tmr_touch_port=5
a = {}
is4=0
time = 0
div_time = 0
-- gpio.mode(tmr_touch_port,gpio.INT,gpio.PULLUP)
write_reg(0x50,0x22,0xa7)
write_reg(0x50,0x27,0x01)
write_reg(0x50,0x21,0x01)

function pin1cb(level)
	div_time=tmr.now()-time

	time = tmr.now()


	if div_time <310000 then
		table.insert(a, div_time)
		is4 = is4+1
		else is4=0 a={} end
		if is4==4 then

			if diode == 0 then pwm.setduty(G,1023) diode =1
				else pwm.setduty(G,0) diode =0 end

				is4=0
				a={}
			end

			tmr.delay(80000)
			write_reg(0x50,0x00,0x00)
		end
		
-- --  ====================================================

-- --  ====================================================
function hsvToRgb(h, s, v, a)
  local r, g, b

  local i = math.floor(h * 6);
  local f = h * 6 - i;
  local p = v * (1 - s);
  local q = v * (1 - f * s);
  local t = v * (1 - (1 - f) * s);

  i = i % 6

  if i == 0 then r, g, b = v, t, p
  elseif i == 1 then r, g, b = q, v, p
  elseif i == 2 then r, g, b = p, v, t
  elseif i == 3 then r, g, b = p, q, v
  elseif i == 4 then r, g, b = t, p, v
  elseif i == 5 then r, g, b = v, p, q
  end

  return r * 1023, g * 1023, b * 1023, a * 1023
end

trig_proximity_id=0

function touch_pwm()
	reg_val =string.byte(read_reg(0x50,0x10))
	touch_val = bit.band(reg_val,127)-bit.band(reg_val,128)
	if touch_val>4 then 
		r,g,b=hsvToRgb(touch_val/127,1,1,1)
	else b=0
		r=0
		g=0 
	end
	pwm.setduty(G,g)
	pwm.setduty(R,r)
	pwm.setduty(B,b) 
end
-- --  ====================================================
-- calibration
write_reg(0x50,0x26,0x01)

gpio.mode(tmr_touch_port, gpio.INPUT)
tmr.alarm(trig_proximity_id, 20, 1, touch_pwm)

