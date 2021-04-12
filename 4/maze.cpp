#include <iostream>
#include <fstream>
#include <vector>
#include <initializer_list>
#include <map>
#include <math.h>
#include <string>

namespace rnd {
    int const a = 1020, b = 1203, m = 1e9 + 7;
    bool initialized = false;
    int seed;

    void init() {
        std::ifstream in("rnd.ota");
        initialized = true;
        in >> seed;
    }

    int get() {
        if (!initialized) {
            init();
        }

        std::ofstream out("rnd.ota");
        out << (seed = (seed * a + b) % m);
        return seed;
    }

    int get_positive() {
        int res = get();
        if (res < 0) {
            res = -res - 1;
        }
        return res;
    }

    int get(int n) {
        return get_positive() % n;
    }

    int get(int x, int y) {
        return get_positive() % (y - x + 1) + x;
    }
}

namespace maze {
    int const N = 300, DIR_QTY = 4;
    bool is_wall[N][N], best_wall[N][N];
    int n, m;

    struct coord {
        int x;
        int y;

        coord() = delete;
        coord(int x, int y) : x(x), y(y) {}
        coord(std::initializer_list<int> a) : x(*a.begin()), y(*(a.begin() + 1)) {}

        coord operator+(coord const& c) {
            return coord(x + c.x, y + c.y);
        }
        bool operator==(coord const& c) {
            return x == c.x && y == c.y;
        }
    };
    coord const ERROR_TYPE = {-1, -1};
    std::ostream &operator<<(std::ostream &out, coord const& c) {
        return out << '(' << c.x << ", " << c.y << ')';
    }


    bool is_valid_coordinate(coord const& c) {
        if (c.x <= 0 || c.y <= 0) {
            return false;
        }
        if (c.x >= 2 * n || c.y >= 2 * m) {
            return false;
        }

        return true;
    }

    coord get_next_step(coord const& c) {
        std::vector<coord> dirs = { {0, 2}, {0, -2}, {2, 0}, {-2, 0} };
        while (!dirs.empty()) {
            int pos = rnd::get(dirs.size());
            coord curr = dirs[pos] + c;

            if (!is_valid_coordinate(curr) || !is_wall[curr.x][curr.y]) {
                dirs.erase(dirs.begin() + pos);
            } else {
                return curr;
            }
        }

        return ERROR_TYPE;
    }

    void find_path(coord &curr) {
        for (int i = 0; i < DIR_QTY; ++i) {
            coord next = get_next_step(curr);
            if (next == ERROR_TYPE) {
                // std::cout << "couldn't go anywhere" << std::endl;
                return;
            }

            is_wall[next.x][next.y] = false;
            is_wall[(next.x + curr.x) / 2][(next.y + curr.y) / 2] = false;
            // std::cout << curr << " -> " << next << std::endl;
            find_path(next);
        }
    }

    void gen() {
        // std::cout << "in gen\n";
        for (int i = 0; i < 2 * n + 1; ++i) {
            for (int j = 0; j < 2 * m + 1; ++j) {
                is_wall[i][j] = true;
            }
        }
        // std::cout << "initialized is_wall\n";

        coord start(rnd::get(1, n) * 2 - 1, rnd::get(1, m) * 2 - 1);
        is_wall[start.x][start.y] = false;
        // std::cout << "start coord is: " << start << std::endl;
        find_path(start);
    }

    char get_symb(int i, int j, bool b) {
        if (i % 2 == 0 && j % 2 == 0) {
            return '+';
        } else if (i % 2 == 1 && j % 2 == 1) {
            return '.';
        } else if (i % 2 == 1 && j % 2 == 0) {
            return (b ? '|' : '.');
        } else {
            return (b ? '-' : '.');
        }
    }

    char get_symb(int i, int j) {
        return get_symb(i, j, is_wall[i][j]);
    }

    void run() {
        std::cout << "Input size of maze: (n * m): ";
        std::cin >> n >> m;

        double best_res = 5.49;
        for (int q = 0; q < 300; ++q) {
            maze::gen();
            std::cout << q << std::endl;

            std::map<std::string, int> cnt;
            const int M = 2 * n + 1;
            for (int i = 0; i < M - 4; i += 2) {
                for (int j = 0; j < M - 4; j += 2) {
                    std::string s = "";
                    for (int k = i; k < i + 5; k++) {
                        for (int m = j; m < j + 5; m++) {
                            s.push_back(get_symb(k, m));
                        }
                    }
                    ++cnt[s];
                }
            }

            double sum = 0;
            for (auto &i : cnt) {
                sum += 1.0 / (abs(20 - i.second) + 1);
            }
            double curr_res = 1 + log(1 + sum);

            if (curr_res > best_res) {
                best_res = curr_res;

                for (int i = 0; i < 2 * n + 1; ++i) {
                    for (int j = 0; j < 2 * m + 1; ++j) {
                        best_wall[i][j] = is_wall[i][j];
                    }
                }
            }
        }

        for (int i = 0; i < 2 * maze::n + 1; ++i) {
            for (int j = 0; j < 2 * maze::m + 1; ++j) {
                if (i % 2 == 0 && j % 2 == 0) {
                    std::cout << '+';
                } else if (i % 2 == 1 && j % 2 == 1) {
                    std::cout << '.';
                } else if (i % 2 == 1 && j % 2 == 0) {
                    std::cout << (best_wall[i][j] ? '|' : '.');
                } else {
                    std::cout << (best_wall[i][j] ? '-' : '.');
                }
                // std::cout << maze::best_wall[i][j];
            }
            std::cout << '\n';
        }
        std::cout << "Result rate: " << best_res << '\n';
    }
}

int main() {
    maze::run();

    return 0;
}