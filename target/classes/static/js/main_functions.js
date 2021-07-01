ymaps.ready(mapInit);
$(document).ready(function () {
    archiveRouteSorting();
});
addLineInTable();

var token = $("meta[name='_csrf']").attr("content");
var multiRoute;
var myMap;
var isInterlockActive = false; //Состояние кнопки "Заблокировать"
var tooFastRequests = false; //Маркер обновления данных: true - запрет обработки, false - обработка разрешена

//Инициализация и настройка элементов Яндекс.Карты
function mapInit() {
    var buttonEditor = new ymaps.control.Button({
        data: {content: "Блокировка"}
    });
    buttonEditor.events.add("deselect", function () {
        // Выключение режима редактирования.
        multiRoute.editor.start({
            addWayPoints: true,
            removeWayPoints: true
        });
        isInterlockActive = false;
    });

    buttonEditor.events.add("select", function () {
        multiRoute.editor.stop();
        isInterlockActive = true;
    });

    // Создадим поисковую строку.
    var inputSearch = new ymaps.control.SearchControl({
        options: {
            size: 'small',
        }
    });

    // Создаем карту с добавленной на нее кнопкой.
    myMap = new ymaps.Map('map', {
        center: [56.051878, 40.164916],
        zoom: 7,
        controls: ['geolocationControl', 'typeSelector', 'zoomControl', inputSearch, buttonEditor]
    }, {
        minZoom: 3,
        maxZoom: 17,
        buttonMaxWidth: 300
    });

    //Ориентируем карту на основе геолокации пользователя
    ymaps.geolocation.get().then(function (res) {
        myMap.setBounds(res.geoObjects.get(0).properties.get('boundedBy'));
    });

    myMap.copyrights.add('© Ruslan Chkalov');

    // Добавляем мультимаршрут на карту.
    multimapReInitCore([]);
}

// Отправка координат меток на карте в MVC
function getData() {
    var len = multiRoute.getWayPoints().toArray().length;
    var points = '';
    for (var i = 0; i < len; i++) {
        var yandexWayPoint = multiRoute.getWayPoints().get(i);
        var coord = yandexWayPoint.geometry.getCoordinates();
        if (coord != null) {
            points += coord + '_';
        }
    }
    $.ajax({
        type: 'POST',
        url: '/requests/getWayPoints',
        headers: {"X-CSRF-TOKEN": token}, //send CSRF token in header
        data: {
            message: points
        }
    }).then(function () {
        autoUpdateTable();
    });
}

//Печать окна браузера
function printRoute() {
    window.print();
}

//Обновление таблицы координат точек раздела "Маршрут"
function autoUpdateTable() {
    $.get("requests/updateTable").done(function (fragment) { // get from controller
        $("#pointsTable").replaceWith(fragment); // update snippet of page
    });
}

//Добавление пустой строки для ввода координат новой точки "Маршрута"
function addLineInTable() {
    $.get("requests/addLineInTable").done(function (fragment) { // get from controller
        $("#pointsTable").replaceWith(fragment); // update snippet of page
    });
}

//Удаление точки маршрута из таблицы и карты
function removeRowFromTable(elementId) {
    if (tooFastRequests == true) {
        return;
    }
    if (elementId == "routeRow0") {
        document.getElementById("routeInput0").setAttribute("value", "");
    } else {
        document.getElementById(elementId).remove();
    }
    getTableValues();
}

//Сохранение текущего маршрута
function saveRoute() {
    var routeName = document.getElementById("routename").value;
    var routeNote = document.getElementById("routenote").value;
    $.ajax({
        type: 'POST',
        url: '/requests/saveRoute',
        headers: {"X-CSRF-TOKEN": token}, //send CSRF token in header
        data: {
            routeName: routeName,
            routeNote: routeNote
        },
        success: function (text) {
            document.getElementById("saveError").style.display = "none";
            document.getElementById("saveOk").style.display = "none";
            if (text == "error1") {
                document.getElementById("saveError").innerHTML = "Маршрут не содержит точек";
                document.getElementById("saveError").style.display = "inline";
            } else if (text == "error2") {
                document.getElementById("saveError").innerHTML = "Не задано название маршрута";
                document.getElementById("saveError").style.display = "inline";
            } else if (text == "error3") {
                document.getElementById("saveError").innerHTML = "Маршрут с подобным именем уже существует";
                document.getElementById("saveError").style.display = "inline";
            } else {
                document.getElementById("saveOk").style.display = "inline";
                archiveRouteSorting();
            }
        }
    })
}

//Обновление данных на карте при изменении данных таблицы
function getTableValues() {
    if (tooFastRequests == true) {
        return;
    }
    var array = [];
    var j = 0;
    for (var i = 0; i < 100; i++) {
        var buffer = document.getElementById("routeInput" + i);
        if (buffer == null || buffer.value == "") {
            continue;
        }
        array[j] = buffer.value;
        j++;
    }
    multimapReInit(array, true);
}

//Очистить список точек таблицы и карты
function cleanMultiRoute() {
    multimapReInit([]);
    getData();
}

//Загрука доступных архивных маршрутов с фильтрацией
function archiveRouteSorting() {
    document.getElementById("sortingSelector").value;
    $.ajax({
        type: 'POST',
        url: '/requests/refreshRouteArchive',
        headers: {"X-CSRF-TOKEN": token}, //send CSRF token in header
        data: {
            sortingVariant: document.getElementById("sortingSelector").value,
        },
        success: function (fragment) {
            $("#archiveRoutesTable").replaceWith(fragment);
        }
    });
}

//Удаление архивного маршрута
function removeArchiveRoute(elementId) {
    document.getElementById("archiveRoute" + elementId).remove();
    $.ajax({
        type: 'POST',
        url: '/requests/removeArchiveRoute',
        headers: {"X-CSRF-TOKEN": token}, //send CSRF token in header
        data: {
            routeId: elementId,
        }
    });
}

//Загрузка архивного маршрута
function loadArchiveRoute(routeId) {
    var array;
    $.ajax({
        type: 'POST',
        url: '/requests/loadArchiveRoute',
        headers: {"X-CSRF-TOKEN": token}, //send CSRF token in header
        data: {
            routeId: routeId
        },
        success: function (receivedArray) {
            array = receivedArray;
        }
    }).then(function () {
        multimapReInit(array, true);
        autoUpdateTable();
    });
}

//Обновление мультимаршрута
function multimapReInit(array, autoBounds) {
    multimapReInitCore(array, autoBounds);
    tooFastRequests = true; //Фикс слишком частых запросов
    document.getElementById("routeLoading").style.display = "block";
    iterableTimeout(array);
}

//Итерационная проверка отклика сервера Яндекса (плавающее время отклика)
var responseError = false; //Статус отклика сервера
var errorCounter = 0; //Счетчик отклика сервера
function iterableTimeout(array) {
    setTimeout(function () { //Проверка обновления данных со стороны сервера Яндекс
        responseError = false;
        var getMultiRouteLength = multiRoute.getWayPoints().toArray().length;
        if (getMultiRouteLength == 1) { //Ошибка в первом запросе игнорируется Яндексом
            if (multiRoute.getWayPoints().get(0).geometry.getCoordinates() == null) {
                responseError = true;
            }
        } else if (array.length != getMultiRouteLength) { //BadRequest при ошибке в запросе
            responseError = true;
        }
        if (responseError == true & errorCounter < 15) {
            errorCounter = errorCounter + 1;
            iterableTimeout(array);
        } else if (responseError == true & errorCounter == 15) {
            errorCounter = 0;
            tooFastRequests = false;
            document.getElementById("routeLoading").style.display = "none";
            document.getElementById("routeError").style.display = "block";
            multimapReInitCore([]);
        } else if (responseError == false) {
            document.getElementById("routeLoading").style.display = "none";
            tooFastRequests = false;
            errorCounter = 0;
        }
    }, 500);
}

//Обновление мультимаршрута (системная процедура)
function multimapReInitCore(array, autoBounds) {
    myMap.geoObjects.remove(multiRoute);
    multiRoute = new ymaps.multiRouter.MultiRoute({
        referencePoints: array
    }, {
        wayPointStart: false,
        editorMidPointsType: "via",
        editorDrawOver: false,
        boundsAutoApply: autoBounds
    });
    myMap.geoObjects.add(multiRoute);
    multiRoute.events.add('update', function () {
        document.getElementById("routeError").style.display = "none";
        getData();
    });
    if (isInterlockActive == false) {
        multiRoute.editor.start({
            addWayPoints: true,
            removeWayPoints: true
        });
    }
}
