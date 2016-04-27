wifi.setmode(wifi.STATION)
-- wifi.sta.config("esp2323", "asd12345")
wifi.sta.config("ISS-1204A", "Nyzarak3132")
-- wifi.sta.config("Xperia Z3 Compact_1e7c", "40055dfe33b5")

proximity_detector_timer_id = 3
proximity_detector_status = false
deviceName = "Device no.1"
ver = "ver. 1.0"
sensitivity_value = 0

-- functions

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

-- LED
R=2
G=8
B=1
pwm.setup(R, 100, 1)
pwm.setup(G, 100, 1)
pwm.setup(B, 100, 1)


cc = coap.Client()		
tmr.alarm(1, 1000, 1, function()
	if wifi.sta.getip()== nil then
		print("IP unavaiable, Waiting...")
	else
		tmr.stop(1)
		broadcast = wifi.sta.getbroadcast()
		register_json = "{ \"title\" : \"" .. deviceName .. "\", \"ip\" : \"" .. wifi.sta.getip() .. "\", \"port\" : " .. 5683 .. ", \"mac\" : \"" .. wifi.ap.getmac() .. "\", \"version\" : \"" .. ver .. "\"}"
		cc:post(coap.CON, "coap://" .. broadcast .. ":5683/register", register_json)
 	end
end)

tmr.alarm(2, 5000, 1, function()
	if UUID == nil or server_ip == nil then
		print("device not active, Waiting...")
	else
		tmr.stop(2)
		active_json = "{ \"uuid\" : \"" .. UUID .. "\"}"
		cc:post(coap.CON, service_notify_active, active_json)
 	end
end)

cs=coap.Server()
cs:listen(5683)
-- server post to device functions
cs:func("uuid")
cs:func("serverAddress")
function uuid(payload)
	print(payload)
	UUID=payload
	respond = "OK"
	return respond
end
function serverAddress(payload)
	server_ip = payload
	service_proximity = server_ip .. "proximity"
	service_register = server_ip .. "register"
	service_notify_active = server_ip .. "active"
	respond = "OK"
	return respond
end

cs:func("proximity_start")
cs:func("proximity_stop")
function proximity_start(payload)
	tmr.start(proximity_detector_timer_id)
	proximity_detector_status = true
	respond = "OK"
	return respond
end
function proximity_stop(payload)
	tmr.stop(proximity_detector_timer_id)
	proximity_detector_status = false
	respond = "OK"
	return respond
end

tmr.register(proximity_detector_timer_id, 250, 1, function()
	if UUID == nil then
		print("UUID unavaiable, need to restart...")
		node.restart() 
	else
		reg_val =string.byte(read_reg(0x50,0x10))
		touch_val = bit.band(reg_val,127)-bit.band(reg_val,128)
		touch_json = "{ \"uuid\" : \"" .. UUID .. "\", \"proximity\" : " .. touch_val .. "}"
		cc:post(coap.NON, service_proximity, touch_json)
	end
end)

cs:func("set_r")
cs:func("set_g")
cs:func("set_b")
cs:func("sensitivity")
function set_r(payload)
	pwm.setduty(R, payload)
	respond = "OK"
	return respond
end
function set_g(payload)
	pwm.setduty(G, payload)
	respond = "OK"
	return respond
end
function set_b(payload)
	pwm.setduty(B, payload)
	respond = "OK"
	return respond
end
function sensitivity(payload)
	sensitivity_value = payload
    node.input("write_reg(0x50,0x1F,0x" .. payload .. "F)")
	respond = "OK"
	return respond
end

cs:func("status")
function status(payload)
	status_json = "{\"uuid\":\""..UUID.."\",\"detector\":"..tostring(proximity_detector_status)..",\"sensitivity\":"..sensitivity_value..",\"r\":"..pwm.getduty(R)..",\"g\":"..pwm.getduty(G)..",\"b\":"..pwm.getduty(B).."}"
	respond = status_json
	return respond
end

cs:func("calibrate")
function calibrate(payload)
	write_reg(0x50,0x26,0x01)
	respond = "OK"
	return respond
end

cs:func("requestRefersh")
function requestRefersh(payload)
	tmr.start(2)
	server_ip = payload
	service_proximity = server_ip .. "proximity"
	service_register = server_ip .. "register"

	register_json = "{ \"title\" : \"" .. deviceName .. "\", \"ip\" : \"" .. wifi.sta.getip() .. "\", \"port\" : " .. 5683 .. ", \"mac\" : \"" .. wifi.ap.getmac() .. "\", \"version\" : \"" .. ver .. "\"}"
	cc:post(coap.CON, service_register, register_json)
	respond = "OK"
	return respond
end

isActive=1
cs:var("isActive")

