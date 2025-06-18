# Пользовательская реализация RxJava

Этот проект представляет собой пользовательскую реализацию концепций реактивного программирования, аналогичную RxJava. Он предоставляет легковесную, простую для понимания библиотеку реактивного программирования с основными функциями для работы с асинхронными потоками данных.

## Содержание
1. [Архитектура](#архитектура)
2. [Основные компоненты](#основные-компоненты)
3. [Планировщики (Schedulers)](#планировщики-schedulers)
4. [Операторы](#операторы)
5. [Обработка ошибок](#обработка-ошибок)
6. [Тестирование](#тестирование)
7. [Примеры использования](#примеры-использования)

## Архитектура

Библиотека следует паттерну Наблюдатель и реализует следующие ключевые компоненты:

### Основные интерфейсы и классы

1. **Observer<T>**
   - Интерфейс для получения уведомлений от Observable
   - Методы:
     - `onNext(T item)`: Получает элементы от Observable
     - `onError(Throwable t)`: Обрабатывает ошибки
     - `onComplete()`: Обрабатывает завершение

2. **Observable<T>**
   - Основной класс для создания и манипуляции потоками данных
   - Поддерживает создание, преобразование и подписку
   - Реализует основной паттерн реактивного программирования

3. **Disposable**
   - Интерфейс для управления подписками
   - Методы:
     - `dispose()`: Отменяет подписку
     - `isDisposed()`: Проверяет статус подписки

## Основные компоненты

### Создание Observable
```java
Observable<Integer> observable = Observable.create(observer -> {
    observer.onNext(1);
    observer.onNext(2);
    observer.onComplete();
});
```

### Подписка
```java
Disposable subscription = observable.subscribe(
    item -> System.out.println("Получено: " + item),
    error -> System.out.println("Ошибка: " + error.getMessage()),
    () -> System.out.println("Завершено!")
);
```

## Планировщики (Schedulers)

Библиотека предоставляет три типа планировщиков для управления выполнением потоков:

1. **IOThreadScheduler**
   - Использует `CachedThreadPool`
   - Идеален для I/O операций
   - Создает новые потоки по мере необходимости
   - Переиспользует неактивные потоки
   - Лучше всего подходит для: сетевых вызовов, операций с файлами

2. **ComputationScheduler**
   - Использует `FixedThreadPool`
   - Размер основан на доступных процессорах
   - Оптимизирован для CPU-интенсивных задач
   - Лучше всего подходит для: обработки данных, вычислений

3. **SingleThreadScheduler**
   - Использует исполнитель с одним потоком
   - Обеспечивает последовательное выполнение
   - Лучше всего подходит для: обновлений UI, упорядоченных операций

### Использование планировщиков
```java
// Подписка в IO потоке
observable.subscribeOn(new IOThreadScheduler())
          .subscribe(...);

// Наблюдение в вычислительном потоке
observable.observeOn(new ComputationScheduler())
          .subscribe(...);
```

## Операторы

### Map
Преобразует элементы, испускаемые Observable
```java
observable.map(x -> x * 2)
          .subscribe(...);
```

### Filter
Фильтрует элементы на основе предиката
```java
observable.filter(x -> x % 2 == 0)
          .subscribe(...);
```

### FlatMap
Преобразует элементы в Observable и объединяет их
```java
observable.flatMap(x -> Observable.create(observer -> {
    observer.onNext(x * 10);
    observer.onNext(x * 20);
    observer.onComplete();
}))
.subscribe(...);
```

## Обработка ошибок

Библиотека предоставляет комплексную обработку ошибок:

1. Распространение ошибок через цепочки операторов
2. Обработка ошибок в Observer
3. Очистка ресурсов при ошибках
4. Обработка прерывания потоков

Пример:
```java
observable.subscribe(
    item -> System.out.println("Получено: " + item),
    error -> System.out.println("Ошибка: " + error.getMessage()),
    () -> System.out.println("Завершено!")
);
```

## Тестирование

Проект включает комплексные модульные тесты, охватывающие:

1. Базовую функциональность Observable
2. Реализации операторов
3. Поведение планировщиков
4. Обработку ошибок
5. Управление подписками

### Категории тестов

1. **ObservableTest**
   - Базовая эмиссия
   - Обработка ошибок
   - Функциональность операторов
   - Поведение Disposable

2. **SchedulerTest**
   - Выполнение потоков
   - Изоляция потоков
   - Переключение планировщиков

3. **OperatorChainTest**
   - Комбинации операторов
   - Распространение ошибок
   - Управление ресурсами

## Примеры использования

### Базовое использование
```java
Observable.create(observer -> {
    observer.onNext(1);
    observer.onNext(2);
    observer.onComplete();
})
.subscribe(
    item -> System.out.println("Получено: " + item),
    error -> System.out.println("Ошибка: " + error.getMessage()),
    () -> System.out.println("Завершено!")
);
```

### Сложная цепочка
```java
Observable.create(observer -> {
    observer.onNext(1);
    observer.onNext(2);
    observer.onNext(3);
    observer.onComplete();
})
.filter(x -> x % 2 == 0)
.map(x -> x * 2)
.flatMap(x -> Observable.create(observer -> {
    observer.onNext(x * 10);
    observer.onNext(x * 20);
    observer.onComplete();
}))
.subscribeOn(new IOThreadScheduler())
.observeOn(new ComputationScheduler())
.subscribe(
    item -> System.out.println("Результат: " + item),
    error -> System.out.println("Ошибка: " + error.getMessage()),
    () -> System.out.println("Завершено!")
);
```

### Обработка ошибок
```java
Observable.create(observer -> {
    try {
        observer.onNext(1);
        throw new RuntimeException("Тестовая ошибка");
    } catch (Exception e) {
        observer.onError(e);
    }
})
.subscribe(
    item -> System.out.println("Получено: " + item),
    error -> System.out.println("Обработана ошибка: " + error.getMessage()),
    () -> System.out.println("Это не будет вызвано")
);
```

### Управление ресурсами
```java
Disposable subscription = Observable.create(observer -> {
    int i = 0;
    while (true) {
        observer.onNext(i++);
        Thread.sleep(100);
    }
})
.subscribe(
    item -> System.out.println("Получено: " + item),
    error -> System.out.println("Ошибка: " + error.getMessage()),
    () -> System.out.println("Это не будет вызвано")
);

// Отмена подписки через 500мс
Thread.sleep(500);
subscription.dispose();
```

## Заключение

Данная реализация предоставляет прочную основу для реактивного программирования с:
- Чистым, интуитивным API
- Комплексной обработкой ошибок
- Гибкой моделью потоков
- Управлением ресурсами
- Обширным покрытием тестами

Она может использоваться как учебное пособие для понимания концепций реактивного программирования или как легковесная альтернатива полнофункциональным реактивным библиотекам. 