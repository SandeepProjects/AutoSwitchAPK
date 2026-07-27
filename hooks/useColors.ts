import { colors, ColorPalette } from '../constants/colors';
import { useTheme } from '../context/ThemeContext';

export const useColors = (): ColorPalette => {
  const { resolved } = useTheme();
  return colors[resolved];
};
