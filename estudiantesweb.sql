-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Servidor: 127.0.0.1:3306
-- Tiempo de generación: 27-05-2025 a las 23:26:03
-- Versión del servidor: 10.4.32-MariaDB
-- Versión de PHP: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de datos: `estudianteweb`
--

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `estudiantesweb`
--

CREATE TABLE `estudiantesweb` (
  `codiEstdWeb` int(11) NOT NULL,
  `ndniEstdWeb` varchar(50) NOT NULL,
  `appaEstdWeb` varchar(50) NOT NULL,
  `apmaEstdWeb` varchar(50) NOT NULL,
  `nombEstdWeb` varchar(50) NOT NULL,
  `fechNaciEstdWeb` date NOT NULL,
  `logiEstd` varchar(100) NOT NULL,
  `passEstd` varchar(500) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `estudiantesweb`
--

INSERT INTO `estudiantesweb` (`codiEstdWeb`, `ndniEstdWeb`, `appaEstdWeb`, `apmaEstdWeb`, `nombEstdWeb`, `fechNaciEstdWeb`, `logiEstd`, `passEstd`) VALUES
(1, '12345679', 'Gonzales', 'Pérez', 'Juan', '2000-05-15', 'juan.gonzales', '123456'),
(2, '87654321', 'Ramos', 'Díaz', 'María', '1999-11-23', 'maria.ramos', 'claveSecreta2');

--
-- Índices para tablas volcadas
--

--
-- Indices de la tabla `estudiantesweb`
--
ALTER TABLE `estudiantesweb`
  ADD PRIMARY KEY (`codiEstdWeb`),
  ADD UNIQUE KEY `ndniEstdWeb` (`ndniEstdWeb`),
  ADD UNIQUE KEY `logiEstd` (`logiEstd`);

--
-- AUTO_INCREMENT de las tablas volcadas
--

--
-- AUTO_INCREMENT de la tabla `estudiantesweb`
--
ALTER TABLE `estudiantesweb`
  MODIFY `codiEstdWeb` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
