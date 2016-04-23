wifi.setmode(wifi.STATION)
-- server_ip = "coap://192.168.43.1:5683/"
-- wifi.sta.config("esp2323", "asd12345")
server_ip = "coap://172.16.1.2:5683/"
wifi.sta.config("ISS-1204A", "Nyzarak3132")

service_proximity = server_ip .. "proximity"
service_register = server_ip .. "register"

proximity_detector_timer_id = 2

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

-- LED
R=2
G=8
B=1
pwm.setup(R, 100, 1)
pwm.setup(G, 100, 1)
pwm.setup(B, 100, 1)



cc = coap.Client()		
-- wait for ip and register this device
tmr.alarm(1, 1000, 1, function()
	if wifi.sta.getip()== nil then
		print("IP unavaiable, Waiting...")
	else
		tmr.stop(1)

		register = {title = "Device no.1", ip = wifi.sta.getip(), port = 5683, mac = wifi.ap.getmac(), version = "ver. 1.0"}
		ok, json = pcall(cjson.encode, register)
		cc:post(coap.CON, service_register, json)
 	end
end)

cs=coap.Server()
cs:listen(5683)
-- server post to device functions
cs:func("uuid")
function uuid(payload)
	print(payload)
	UUID=payload
	respond = "OK"
	return respond
end

-- +-------------------------------+
-- |                               |
-- |      PROXIMITY DETECTOR       |
-- |                               |
-- +-------------------------------+
cs:func("proximity_start")
cs:func("proximity_stop")
function proximity_start(payload)
	tmr.start(proximity_detector_timer_id)
	respond = "OK"
	return respond
end
function proximity_stop(payload)
	tmr.stop(proximity_detector_timer_id)
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

-- +--------------------+
-- |                    |
-- |      RGB LEDs      |
-- |                    |
-- +--------------------+
cs:func("set_r")
cs:func("set_g")
cs:func("set_b")
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

-- +--------------------+
-- |                    |
-- |    Device status   |
-- |                    |
-- +--------------------+
cs:func("status")
function status(payload)
	status_json = "{ \"uuid\" : \"" .. UUID .. "\", \"r\" : " .. pwm.getduty(R) .. ", \"g\" : " .. pwm.getduty(G) .. ", \"b\" : " .. pwm.getduty(B) .. "}"
	-- cc:post(coap.NON, service_proximity, status_json)
	respond = status_json
	return respond
end