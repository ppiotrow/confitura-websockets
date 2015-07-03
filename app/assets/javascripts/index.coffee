$ ->
  isInitialised = false
  ws = new WebSocket $("body").data("ws-url")
  ws.onopen = () ->
    ws.send("STATUS")
  ws.onmessage = (event) ->
    message = JSON.parse event.data
    switch message.statusType
      when "wallet"
        updateBalance(message.balance)
      when "auction"
        updateAuction(message)
        if(message.isFinished)
          finish()
  ws.onclose = () ->
    disconnected()
  ws.onerror = () ->
    disconnected()

  updateBalance = (balance) ->
    $("#balance").text(balance)
  updateAuction = (msg) ->
    init(msg) if !isInitialised
    setClock(msg.millis/1000)
    price = $("#price")
    price.fadeOut 300, ->
      price.text("$" + msg.price).fadeIn()
    $("#winner").text(msg.winner)

  init = (msg) ->
    isInitialised = true
    $("#title").text(msg.title)
    $("#itemImage").hide().attr("src", $("body").data("img-store") + msg.img).fadeIn(900);
  finish = () ->
    setClock(0)
    $('#bid_button').prop('disabled', true);
  $("#bid_button").click (event) ->
    ws.send("BID")

  setClock = (ms) ->
   $("#timer").FlipClock ms, {countdown: true,clockFace: 'MinuteCounter'}
  disconnected = () ->
    toastr.options = {
          "positionClass": "toast-top-center",
          "timeOut": "0",
          "extendedTimeOut": "0",
          "tapToDismiss" : false
     }
    toastr["error"]("There was problem with this penny auction. Please refresh your browser")

